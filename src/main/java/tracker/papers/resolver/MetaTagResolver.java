package tracker.papers.resolver;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import tracker.data.PaperSource;

/**
 * Last resort for links the registries don't know: reads the citation_* meta tags that publishers put on
 * their article pages. If the page names a DOI, Crossref is asked for it so the exact registry data wins.
 */
@Component
@Order(3)
public class MetaTagResolver implements PaperResolver {

    private static final String USER_AGENT = "Mozilla/5.0 (compatible; RamanLabTracker/0.1)";
    private static final int TIMEOUT_MS = 10_000;
    // pages we are not allowed to read or that no longer exist: not worth retrying
    private static final Set<Integer> UNREADABLE = Set.of(401, 403, 404, 410);
    // "2015/05/27", "2015-05" or "2019 May 3"
    private static final Pattern DATE = Pattern.compile("^(\\d{4})(?:[/ -]([A-Za-z]{3,}|\\d{1,2}))?(?:[/ -](\\d{1,2}))?");

    private final CrossrefResolver crossref;

    /**
     * Creates the resolver.
     *
     * @param crossref used when a page names a DOI, so the registry's data wins
     */
    public MetaTagResolver(CrossrefResolver crossref) {
        this.crossref = crossref;
    }

    @Override
    public boolean supports(String normalizedUrl) {
        return normalizedUrl.startsWith("http");
    }

    @Override
    public Optional<ResolvedMetadata> resolve(String normalizedUrl) {
        Document page = fetch(normalizedUrl);
        if (page == null) {
            return Optional.empty();
        }
        String title = meta(page, "citation_title");
        if (title.isBlank()) {
            return Optional.empty();
        }

        String doi = cleanDoi(meta(page, "citation_doi"));
        if (!doi.isEmpty()) {
            Optional<ResolvedMetadata> exact = crossref.resolve("https://doi.org/" + doi);
            if (exact.isPresent()) {
                return exact;
            }
        }
        return Optional.of(new ResolvedMetadata(
                title,
                page.select("meta[name=citation_author]").eachAttr("content").stream()
                        .map(MetaTagResolver::firstNameFirst)
                        .filter(name -> !name.isBlank())
                        .toList(),
                publishedOn(page),
                doi.isEmpty() ? null : doi,
                PaperSource.META_TAGS));
    }

    /** Null when there is no readable web page (a bare PDF, or access refused). */
    private static Document fetch(String url) {
        try {
            return Jsoup.connect(url).userAgent(USER_AGENT).timeout(TIMEOUT_MS).followRedirects(true).get();
        } catch (UnsupportedMimeTypeException e) {
            return null;
        } catch (HttpStatusException e) {
            if (UNREADABLE.contains(e.getStatusCode())) {
                return null;
            }
            throw new UncheckedIOException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String meta(Document page, String name) {
        return page.select("meta[name=" + name + "]").attr("content").trim();
    }

    private static String cleanDoi(String doi) {
        return doi.replaceFirst("(?i)^(doi:|https?://(dx\\.)?doi\\.org/)", "").trim().toLowerCase();
    }

    /** Publishers write "Last, First"; Crossref and arXiv give "First Last", so match them. */
    private static String firstNameFirst(String name) {
        List<String> parts = List.of(name.split(",", -1));
        return parts.size() == 2 ? (parts.get(1).trim() + " " + parts.get(0).trim()).trim() : name.trim();
    }

    /** A number ("05") or a month name ("May"); anything else, such as "Spring", counts as January. */
    private static int month(String text) {
        if (text == null) {
            return 1;
        }
        if (Character.isDigit(text.charAt(0))) {
            return Integer.parseInt(text);
        }
        String start = text.substring(0, 3).toUpperCase();
        for (Month month : Month.values()) {
            if (month.name().startsWith(start)) {
                return month.getValue();
            }
        }
        return 1;
    }

    private static LocalDate publishedOn(Document page) {
        for (String tag : List.of("citation_publication_date", "citation_date", "citation_online_date")) {
            Matcher date = DATE.matcher(meta(page, tag));
            if (date.find()) {
                try {
                    return LocalDate.of(Integer.parseInt(date.group(1)), month(date.group(2)),
                            date.group(3) == null ? 1 : Integer.parseInt(date.group(3)));
                } catch (DateTimeException e) {
                    return null;
                }
            }
        }
        return null;
    }
}

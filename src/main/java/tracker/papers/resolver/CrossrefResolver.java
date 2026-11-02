package tracker.papers.resolver;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tracker.data.PaperSource;

/**
 * Looks up DOI links ({@code https://doi.org/...}) in the Crossref registry.
 */
@Component
@Order(1)
public class CrossrefResolver implements PaperResolver {

    private static final String DOI_PREFIX = "https://doi.org/";
    // review articles can cite hundreds of works; more than this is not useful on one page
    private static final int MAX_REFERENCES = 500;

    private final RestClient http;
    private final JsonMapper json = JsonMapper.builder().build();

    /**
     * Creates the resolver.
     *
     * @param baseUrl the Crossref API's address, from {@code papers.crossref-url}; tests point it at a stub
     */
    public CrossrefResolver(@Value("${papers.crossref-url:https://api.crossref.org}") String baseUrl) {
        this.http = ResolverHttp.client(baseUrl);
    }

    @Override
    public boolean supports(String normalizedUrl) {
        return normalizedUrl.startsWith(DOI_PREFIX);
    }

    @Override
    public Optional<ResolvedMetadata> resolve(String normalizedUrl) {
        String doi = normalizedUrl.substring(DOI_PREFIX.length());
        String body;
        try {
            body = http.get().uri("/works/{doi}", doi).retrieve().body(String.class);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }

        JsonNode work = json.readTree(body).path("message");
        JsonNode titles = work.path("title");
        if (titles.isEmpty() || titles.get(0).asString().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new ResolvedMetadata(
                plainText(titles.get(0).asString()),
                authors(work.path("author")),
                publishedOn(work.path("issued").path("date-parts")),
                work.path("DOI").asString(null),
                PaperSource.REGISTRY,
                references(work.path("reference"))));
    }

    /**
     * Crossref's reference list, when the publisher deposited one. Each entry usually has an "unstructured" citation;
     * when it doesn't, one is put together from the author, year and titles.
     */
    private static List<ResolvedReference> references(JsonNode entries) {
        List<ResolvedReference> references = new ArrayList<>();
        for (JsonNode entry : entries) {
            String citation = entry.path("unstructured").asString("").strip();
            if (citation.isEmpty()) {
                citation = String.join(", ", Stream.of("author", "year", "article-title", "journal-title", "volume-title")
                        .map(field -> entry.path(field).asString("").strip())
                        .filter(part -> !part.isEmpty())
                        .toList());
            }
            String doi = entry.path("DOI").asString("").strip().toLowerCase();
            if (!citation.isEmpty() && references.size() < MAX_REFERENCES) {
                references.add(new ResolvedReference(plainText(citation), doi.isEmpty() ? null : doi));
            }
        }
        return references;
    }

    /** Crossref titles can carry HTML such as {@code <i>} and line breaks; keep only the text. */
    private static String plainText(String title) {
        return title.replaceAll("<[^>]+>", "").replaceAll("\\s+", " ").trim();
    }

    private static List<String> authors(JsonNode authors) {
        List<String> names = new ArrayList<>();
        for (JsonNode author : authors) {
            // organisations have only "name"; people have "given" and "family"
            String name = author.has("name")
                    ? author.path("name").asString()
                    : (author.path("given").asString("") + " " + author.path("family").asString("")).trim();
            if (!name.isBlank()) {
                names.add(name);
            }
        }
        return names;
    }

    /** Crossref dates may hold only a year or a year and month; missing parts default to 1. */
    private static LocalDate publishedOn(JsonNode dateParts) {
        JsonNode parts = dateParts.path(0);
        if (!parts.path(0).isInt()) {
            return null;
        }
        return LocalDate.of(parts.path(0).asInt(), parts.path(1).asInt(1), parts.path(2).asInt(1));
    }
}

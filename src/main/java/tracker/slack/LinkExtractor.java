package tracker.slack;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Finds paper links in Slack message text and normalises them. Anything that isn't a paper link is ignored. */
public class LinkExtractor {

    // Stops at whitespace, '<', '>' and '|', so Slack's <url|label> and <url> forms both yield just the url
    private static final Pattern URL = Pattern.compile("https?://[^\\s<>|]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern ARXIV_PATH = Pattern.compile(
            "^/(?:abs|pdf)/(\\d{4}\\.\\d{4,5}|[a-z-]+(?:\\.[a-z]{2})?/\\d{7})(?:v\\d+)?(?:\\.pdf)?$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DOI = Pattern.compile("10\\.\\d{4,9}/.+");
    // Nature article ids are the DOI suffix under the 10.1038 prefix
    private static final Pattern NATURE_PATH = Pattern.compile("^/articles/([^/]+)");
    private static final Pattern IEEE_PATH = Pattern.compile("^/(?:abstract/)?document/(\\d+)/?$");

    private static final Set<String> DOI_HOSTS = Set.of(
            "doi.org", "dx.doi.org", "link.springer.com", "dl.acm.org", "onlinelibrary.wiley.com");
    private static final Set<String> TRACKING_PARAMS = Set.of("fbclid", "gclid", "mc_cid", "mc_eid", "igshid");

    public List<PaperLink> extract(String slackText) {
        if (slackText == null) {
            return List.of();
        }
        Map<String, PaperLink> found = new LinkedHashMap<>();
        Matcher matcher = URL.matcher(slackText);
        while (matcher.find()) {
            boolean wrappedBySlack = matcher.start() > 0 && slackText.charAt(matcher.start() - 1) == '<';
            String raw = matcher.group().replace("&amp;", "&");
            String url = wrappedBySlack ? raw : stripTrailingPunctuation(raw);
            normalize(url).ifPresent(normalized -> found.putIfAbsent(normalized, new PaperLink(url, normalized)));
        }
        return List.copyOf(found.values());
    }

    private static Optional<String> normalize(String url) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
        if (uri.getHost() == null) {
            return Optional.empty();
        }
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        String path = uri.getPath() == null ? "" : uri.getPath();

        if (host.equals("arxiv.org") || host.endsWith(".arxiv.org")) {
            Matcher arxiv = ARXIV_PATH.matcher(path);
            return arxiv.matches() ? Optional.of("https://arxiv.org/abs/" + arxiv.group(1)) : Optional.empty();
        }
        if (host.equals("nature.com") || host.equals("www.nature.com")) {
            Matcher nature = NATURE_PATH.matcher(path);
            if (nature.find()) {
                return Optional.of(doiUrl("10.1038/" + nature.group(1)));
            }
        }
        if (DOI_HOSTS.contains(host)) {
            Matcher doi = DOI.matcher(path);
            if (doi.find()) {
                return Optional.of(doiUrl(doi.group()));
            }
        }
        if (host.equals("ieeexplore.ieee.org")) {
            Matcher ieee = IEEE_PATH.matcher(path);
            if (ieee.matches()) {
                return Optional.of("https://ieeexplore.ieee.org/document/" + ieee.group(1));
            }
        }
        if (path.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            return Optional.of("https://" + host + (uri.getPort() == -1 ? "" : ":" + uri.getPort())
                    + path + withoutTrackingParams(uri.getRawQuery()));
        }
        return Optional.empty();
    }

    // DOIs are case-insensitive; Springer's PDF links end in ".pdf" after the DOI
    private static String doiUrl(String doi) {
        String clean = doi.toLowerCase(Locale.ROOT).replaceAll("(\\.pdf)?/?$", "");
        return "https://doi.org/" + clean;
    }

    private static String withoutTrackingParams(String rawQuery) {
        if (rawQuery == null) {
            return "";
        }
        String kept = Arrays.stream(rawQuery.split("&"))
                .filter(param -> !isTracking(param))
                .collect(Collectors.joining("&"));
        return kept.isEmpty() ? "" : "?" + kept;
    }

    private static boolean isTracking(String param) {
        String name = param.split("=", 2)[0].toLowerCase(Locale.ROOT);
        return name.startsWith("utm_") || TRACKING_PARAMS.contains(name);
    }

    private static String stripTrailingPunctuation(String url) {
        int end = url.length();
        while (end > 0 && ".,;:!?)]}'\"".indexOf(url.charAt(end - 1)) >= 0) {
            end--;
        }
        return url.substring(0, end);
    }
}

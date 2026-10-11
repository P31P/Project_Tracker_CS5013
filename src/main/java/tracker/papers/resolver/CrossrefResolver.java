package tracker.papers.resolver;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Looks up DOI links (https://doi.org/...) in the Crossref registry. */
@Component
public class CrossrefResolver implements PaperResolver {

    private static final String DOI_PREFIX = "https://doi.org/";

    private final RestClient http;
    private final JsonMapper json = JsonMapper.builder().build();

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
                work.path("DOI").asString(null)));
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

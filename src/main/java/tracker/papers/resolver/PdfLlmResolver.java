package tracker.papers.resolver;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tracker.data.PaperSource;
import tracker.llm.LlmClient;

/**
 * Last rung of the ladder, for bare PDF links nothing else could identify: reads the text of the first two pages
 * and asks the language model for the title, authors, year and DOI. Runs only when {@code llm.enabled} is on.
 * If the model finds a DOI that Crossref knows, the registry's data wins; otherwise the paper is marked
 * {@link PaperSource#MODEL} so the page asks people to check it.
 */
@Component
@Order(4)
public class PdfLlmResolver implements PaperResolver {

    /** Largest PDF downloaded; bigger files are left unresolved. */
    static final int MAX_PDF_BYTES = 20 * 1024 * 1024;
    /** Text sent to the model; the title and authors are always near the start. */
    static final int MAX_TEXT_CHARS = 8000;

    private static final int PAGES = 2;
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final String USER_AGENT = "Mozilla/5.0 (compatible; RamanLabTracker/0.1)";
    // files we are not allowed to read or that no longer exist: not worth retrying
    private static final Set<Integer> UNREADABLE = Set.of(401, 403, 404, 410);
    private static final byte[] PDF_MAGIC = "%PDF-".getBytes(StandardCharsets.US_ASCII);
    private static final int MAX_TITLE_LENGTH = 300;
    private static final int MAX_AUTHORS = 50;

    private static final Map<String, Object> SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "title", Map.of("type", "string"),
                    "authors", Map.of("type", "array", "items", Map.of("type", "string")),
                    "year", Map.of("type", "string"),
                    "doi", Map.of("type", "string")),
            "required", List.of("title", "authors", "year", "doi"),
            "additionalProperties", false);

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final LlmClient llm;
    private final CrossrefResolver crossref;
    private final int maxPdfBytes;
    private final String prompt;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * Creates the resolver and reads the prompt from {@code prompts/pdf-metadata.txt}.
     *
     * @param llm asks the language model
     * @param crossref used when the model finds a DOI, so the registry's data wins
     */
    @Autowired
    public PdfLlmResolver(LlmClient llm, CrossrefResolver crossref) {
        this(llm, crossref, MAX_PDF_BYTES);
    }

    /**
     * Creates the resolver with a different size limit, so tests don't need a 20 MB file.
     *
     * @param llm asks the language model
     * @param crossref used when the model finds a DOI
     * @param maxPdfBytes the largest PDF to download
     */
    PdfLlmResolver(LlmClient llm, CrossrefResolver crossref, int maxPdfBytes) {
        this.llm = llm;
        this.crossref = crossref;
        this.maxPdfBytes = maxPdfBytes;
        try (InputStream in = new ClassPathResource("prompts/pdf-metadata.txt").getInputStream()) {
            this.prompt = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read prompts/pdf-metadata.txt", e);
        }
    }

    @Override
    public boolean supports(String normalizedUrl) {
        return llm.isEnabled() && normalizedUrl.startsWith("http");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Network trouble and an unreachable model are thrown, so the worker tries again later. A file that isn't a
     * PDF, is too big, has no text (a scan) or that the model can't read gives empty.
     */
    @Override
    public Optional<ResolvedMetadata> resolve(String normalizedUrl) {
        byte[] pdf = download(normalizedUrl);
        if (pdf == null) {
            return Optional.empty();
        }
        String text = firstPagesText(pdf);
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        if (text.length() > MAX_TEXT_CHARS) {
            text = text.substring(0, MAX_TEXT_CHARS);
        }
        // an LlmException here is a RuntimeException: the worker counts it as a failed attempt and retries
        String answer = llm.completeJson(prompt + text, SCHEMA);
        return read(answer);
    }

    /** The model's answer as paper details; empty if it isn't JSON or has no title. */
    private Optional<ResolvedMetadata> read(String answer) {
        JsonNode node;
        try {
            node = JSON.readTree(answer);
        } catch (JacksonException e) {
            return Optional.empty();
        }
        String title = text(node.path("title"));
        if (title == null) {
            return Optional.empty();
        }
        String doi = doi(text(node.path("doi")));
        if (doi != null) {
            Optional<ResolvedMetadata> exact = crossref.resolve("https://doi.org/" + doi);
            if (exact.isPresent()) {
                return exact;
            }
        }
        List<String> authors = new ArrayList<>();
        for (JsonNode author : node.path("authors")) {
            String name = text(author);
            if (name != null && authors.size() < MAX_AUTHORS) {
                authors.add(name);
            }
        }
        return Optional.of(new ResolvedMetadata(
                title.length() > MAX_TITLE_LENGTH ? title.substring(0, MAX_TITLE_LENGTH).strip() : title,
                authors, year(text(node.path("year"))), doi, PaperSource.MODEL));
    }

    /** The file's bytes, or null if it can't be read, isn't a PDF or is too big. */
    private byte[] download(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(TIMEOUT)
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();
        try {
            HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                int status = response.statusCode();
                if (UNREADABLE.contains(status)) {
                    return null;
                }
                if (status >= 400) {
                    throw new IOException("HTTP " + status + " from " + url);
                }
                // check the first bytes before downloading the rest, so web pages cost almost nothing
                byte[] start = body.readNBytes(PDF_MAGIC.length);
                if (!Arrays.equals(start, PDF_MAGIC)) {
                    return null;
                }
                byte[] rest = body.readNBytes(maxPdfBytes - start.length + 1);
                if (start.length + rest.length > maxPdfBytes) {
                    return null;
                }
                byte[] pdf = new byte[start.length + rest.length];
                System.arraycopy(start, 0, pdf, 0, start.length);
                System.arraycopy(rest, 0, pdf, start.length, rest.length);
                return pdf;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while downloading " + url, e);
        }
    }

    /** The text of the first pages, or null for a broken or password-protected PDF. */
    private static String firstPagesText(byte[] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(PAGES);
            return stripper.getText(document);
        } catch (IOException e) {
            return null;
        }
    }

    // a string field's trimmed text, or null if it is missing, not a string, or blank
    private static String text(JsonNode field) {
        if (!field.isString() || field.asString().isBlank()) {
            return null;
        }
        return field.asString().strip();
    }

    /** A DOI in the form {@code LinkExtractor} stores (lowercase, no prefix), or null if it doesn't look like one. */
    private static String doi(String raw) {
        if (raw == null) {
            return null;
        }
        String doi = raw.replaceFirst("(?i)^(doi:\\s*|https?://(dx\\.)?doi\\.org/)", "").strip().toLowerCase();
        return doi.matches("10\\.\\d{4,9}/\\S+") ? doi : null;
    }

    /** January 1st of the year the model gave, or null if it isn't a plausible year. */
    private static LocalDate year(String raw) {
        if (raw == null || !raw.matches("\\d{4}")) {
            return null;
        }
        int year = Integer.parseInt(raw);
        return year >= 1900 && year <= Year.now().getValue() + 1 ? LocalDate.of(year, 1, 1) : null;
    }
}

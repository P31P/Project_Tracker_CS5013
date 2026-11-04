package tracker.papers.resolver;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tracker.data.PaperSource;
import tracker.llm.LlmClient;
import tracker.llm.LlmException;
import tracker.support.TestPdfs;

// one WireMock server plays both the website that hosts the PDF and the language model
class PdfLlmResolverTest {

    private static final byte[] PAPER = TestPdfs.withLines("Attention Is All You Need",
            "Ashish Vaswani, Noam Shazeer", "Google Brain", "2017");

    private WireMockServer server;
    private CrossrefResolver crossref;
    private String pdfUrl;

    @BeforeEach
    void setUp() {
        server = new WireMockServer(options().dynamicPort());
        server.start();
        crossref = mock(CrossrefResolver.class);
        when(crossref.resolve(anyString())).thenReturn(Optional.empty());
        pdfUrl = server.baseUrl() + "/paper.pdf";
    }

    @AfterEach
    void stopServer() {
        server.stop();
    }

    private PdfLlmResolver resolver(boolean llmEnabled, int maxPdfBytes) {
        LlmClient llm = new LlmClient(llmEnabled, server.baseUrl() + "/v1", "llama3.2:3b", "", Duration.ofSeconds(5));
        return new PdfLlmResolver(llm, crossref, maxPdfBytes);
    }

    private PdfLlmResolver resolver() {
        return resolver(true, PdfLlmResolver.MAX_PDF_BYTES);
    }

    private void servePdf(byte[] pdf) {
        server.stubFor(get(urlEqualTo("/paper.pdf")).willReturn(aResponse()
                .withHeader("Content-Type", "application/pdf").withBody(pdf)));
    }

    private void modelAnswers(String answer) {
        String reply = JsonMapper.builder().build().writeValueAsString(
                Map.of("choices", List.of(Map.of("message", Map.of("role", "assistant", "content", answer)))));
        server.stubFor(post(urlEqualTo("/v1/chat/completions")).willReturn(okJson(reply)));
    }

    private int modelCalls() {
        return server.findAll(postRequestedFor(urlEqualTo("/v1/chat/completions"))).size();
    }

    @Test
    void readsTheDetailsOfABarePdfWithTheModel() {
        servePdf(PAPER);
        modelAnswers("{\"title\": \"Attention Is All You Need\", \"authors\": [\"Ashish Vaswani\", \" Noam Shazeer \", \"\"],"
                + " \"year\": \"2017\", \"doi\": \"\"}");

        ResolvedMetadata paper = resolver().resolve(pdfUrl).orElseThrow();

        assertThat(paper.title()).isEqualTo("Attention Is All You Need");
        assertThat(paper.authors()).containsExactly("Ashish Vaswani", "Noam Shazeer");
        assertThat(paper.publishedOn()).isEqualTo(LocalDate.of(2017, 1, 1));
        assertThat(paper.doi()).isNull();
        assertThat(paper.source()).isEqualTo(PaperSource.MODEL);
        // the model is given the text of the PDF, not the file
        server.verify(postRequestedFor(urlEqualTo("/v1/chat/completions"))
                .withRequestBody(containing("Attention Is All You Need"))
                .withRequestBody(containing("Google Brain")));
    }

    @Test
    void aDoiTheModelFindsIsLookedUpInCrossrefAndTheRegistryDataWins() {
        servePdf(PAPER);
        modelAnswers("{\"title\": \"A guess\", \"authors\": [], \"year\": \"\", \"doi\": \"https://doi.org/10.1000/XYZ\"}");
        ResolvedMetadata registry = new ResolvedMetadata("Registry title", List.of("A B"), null, "10.1000/xyz",
                PaperSource.REGISTRY);
        when(crossref.resolve("https://doi.org/10.1000/xyz")).thenReturn(Optional.of(registry));

        assertThat(resolver().resolve(pdfUrl)).contains(registry);
    }

    @Test
    void aDoiCrossrefDoesNotKnowIsKeptWithTheModelsDetails() {
        servePdf(PAPER);
        modelAnswers("{\"title\": \"A guess\", \"authors\": [], \"year\": \"\", \"doi\": \"10.1000/unknown\"}");

        ResolvedMetadata paper = resolver().resolve(pdfUrl).orElseThrow();

        assertThat(paper.doi()).isEqualTo("10.1000/unknown");
        assertThat(paper.source()).isEqualTo(PaperSource.MODEL);
    }

    @Test
    void implausibleYearAndDoiAreDropped() {
        servePdf(PAPER);
        modelAnswers("{\"title\": \"A guess\", \"authors\": [], \"year\": \"1066\", \"doi\": \"not a doi\"}");

        ResolvedMetadata paper = resolver().resolve(pdfUrl).orElseThrow();

        assertThat(paper.publishedOn()).isNull();
        assertThat(paper.doi()).isNull();
    }

    @Test
    void answerWithoutATitleGivesEmpty() {
        servePdf(PAPER);
        modelAnswers("{\"title\": \"  \", \"authors\": [\"Someone\"], \"year\": \"2017\", \"doi\": \"\"}");

        assertThat(resolver().resolve(pdfUrl)).isEmpty();
    }

    @Test
    void answerThatIsNotJsonGivesEmpty() {
        servePdf(PAPER);
        modelAnswers("Sorry, I cannot help with that.");

        assertThat(resolver().resolve(pdfUrl)).isEmpty();
    }

    @Test
    void aWebPageIsNotSentToTheModel() {
        server.stubFor(get(urlEqualTo("/paper.pdf")).willReturn(aResponse()
                .withHeader("Content-Type", "text/html").withBody("<html><body>Not a PDF</body></html>")));

        assertThat(resolver().resolve(pdfUrl)).isEmpty();
        assertThat(modelCalls()).isZero();
    }

    @Test
    void aPdfWithoutTextIsNotSentToTheModel() {
        servePdf(TestPdfs.withLines());

        assertThat(resolver().resolve(pdfUrl)).isEmpty();
        assertThat(modelCalls()).isZero();
    }

    @Test
    void aPdfOverTheSizeLimitIsNotRead() {
        servePdf(PAPER);

        assertThat(resolver(true, PAPER.length - 1).resolve(pdfUrl)).isEmpty();
        assertThat(modelCalls()).isZero();
    }

    @Test
    void aBrokenPdfGivesEmpty() {
        servePdf("%PDF-1.7 but nothing else".getBytes(StandardCharsets.US_ASCII));

        assertThat(resolver().resolve(pdfUrl)).isEmpty();
        assertThat(modelCalls()).isZero();
    }

    @Test
    void aDeadLinkGivesEmpty() {
        server.stubFor(get(urlEqualTo("/paper.pdf")).willReturn(aResponse().withStatus(404)));

        assertThat(resolver().resolve(pdfUrl)).isEmpty();
    }

    @Test
    void serverTroubleIsThrownSoTheWorkerRetries() {
        server.stubFor(get(urlEqualTo("/paper.pdf")).willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> resolver().resolve(pdfUrl)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void anUnreachableModelIsThrownSoTheWorkerRetries() {
        servePdf(PAPER);
        server.stubFor(post(urlEqualTo("/v1/chat/completions")).willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> resolver().resolve(pdfUrl)).isInstanceOf(LlmException.class);
    }

    @Test
    void withTheModelOffTheResolverIsSkipped() {
        assertThat(resolver(false, PdfLlmResolver.MAX_PDF_BYTES).supports(pdfUrl)).isFalse();
        assertThat(resolver().supports(pdfUrl)).isTrue();
    }
}

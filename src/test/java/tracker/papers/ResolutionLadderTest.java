package tracker.papers;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import tracker.data.Paper;
import tracker.data.PaperRepository;
import tracker.data.PaperSource;
import tracker.data.PaperStatus;
import tracker.support.Fixtures;
import tracker.support.PostgresTestBase;
import tracker.support.TestPdfs;

/**
 * The whole resolution ladder with every rung in its real order: one WireMock server plays Crossref, a publisher's
 * page, a site hosting a bare PDF, and the language model.
 */
@Transactional
class ResolutionLadderTest extends PostgresTestBase {

    private static final WireMockServer WEB = start();

    private static WireMockServer start() {
        WireMockServer server = new WireMockServer(options().dynamicPort());
        server.start();
        return server;
    }

    @DynamicPropertySource
    static void services(DynamicPropertyRegistry registry) {
        registry.add("papers.crossref-url", WEB::baseUrl);
        registry.add("papers.arxiv-url", WEB::baseUrl);
        registry.add("llm.enabled", () -> "true");
        registry.add("llm.base-url", () -> WEB.baseUrl() + "/v1");
    }

    @AfterAll
    static void stop() {
        WEB.stop();
    }

    @Autowired
    private PaperResolutionService resolution;

    @Autowired
    private PaperRepository papers;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void resetStubs() {
        // anything not stubbed answers 404, which Crossref and the web pages treat as "not found"
        WEB.resetAll();
    }

    @Test
    void knownDoiGetsTheExactRegistryData() {
        WEB.stubFor(get(urlEqualTo("/works/10.1007%2Fs11263-015-0816-y"))
                .willReturn(okJson(Fixtures.read("crossref/springer-imagenet.json"))));

        Paper paper = resolve("https://doi.org/10.1007/s11263-015-0816-y");

        assertThat(paper.getStatus()).isEqualTo(PaperStatus.RESOLVED);
        assertThat(paper.getSource()).isEqualTo(PaperSource.REGISTRY);
        assertThat(paper.getTitle()).isEqualTo("ImageNet Large Scale Visual Recognition Challenge");
        assertThat(paper.getDoi()).isEqualTo("10.1007/s11263-015-0816-y");
    }

    @Test
    void publisherPageGetsItsMetaTags() {
        WEB.stubFor(get(urlEqualTo("/article")).willReturn(aResponse()
                .withHeader("Content-Type", "text/html").withBody(Fixtures.read("html/nature-article.html"))));

        Paper paper = resolve(WEB.baseUrl() + "/article");

        assertThat(paper.getStatus()).isEqualTo(PaperStatus.RESOLVED);
        assertThat(paper.getSource()).isEqualTo(PaperSource.META_TAGS);
        assertThat(paper.getTitle()).isEqualTo("Deep learning");
    }

    @Test
    void barePdfIsReadByTheModel() {
        WEB.stubFor(get(urlEqualTo("/paper.pdf")).willReturn(aResponse().withHeader("Content-Type", "application/pdf")
                .withBody(TestPdfs.withLines("Flux sampling on genome-scale models", "Jane Doe", "2021"))));
        WEB.stubFor(post(urlEqualTo("/v1/chat/completions")).willReturn(okJson(modelReply(
                "{\"title\": \"Flux sampling on genome-scale models\", \"authors\": [\"Jane Doe\"],"
                        + " \"year\": \"2021\", \"doi\": \"\"}"))));

        Paper paper = resolve(WEB.baseUrl() + "/paper.pdf");

        assertThat(paper.getStatus()).isEqualTo(PaperStatus.RESOLVED);
        assertThat(paper.getSource()).isEqualTo(PaperSource.MODEL);
        assertThat(paper.getTitle()).isEqualTo("Flux sampling on genome-scale models");
        assertThat(paper.getAuthors()).containsExactly("Jane Doe");
        assertThat(paper.getPublishedOn()).isEqualTo(LocalDate.of(2021, 1, 1));
    }

    @Test
    void deadLinkBecomesUnresolved() {
        Paper paper = resolve(WEB.baseUrl() + "/gone");

        assertThat(paper.getStatus()).isEqualTo(PaperStatus.UNRESOLVED);
        assertThat(paper.getSource()).isNull();
    }

    private Paper resolve(String normalizedUrl) {
        Paper paper = papers.saveAndFlush(new Paper(normalizedUrl));
        assertThat(resolution.resolveNext()).isTrue();
        papers.flush();
        entityManager.clear();
        return papers.findById(paper.getId()).orElseThrow();
    }

    private static String modelReply(String answer) {
        return JsonMapper.builder().build().writeValueAsString(
                Map.of("choices", List.of(Map.of("message", Map.of("role", "assistant", "content", answer)))));
    }
}

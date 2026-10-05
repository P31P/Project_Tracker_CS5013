package tracker.papers.resolver;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tracker.support.Fixtures;

class CrossrefResolverTest {

    private static final String SPRINGER_URL = "https://doi.org/10.1007/s11263-015-0816-y";
    private static final String SPRINGER_PATH = "/works/10.1007%2Fs11263-015-0816-y";

    private WireMockServer crossref;
    private CrossrefResolver resolver;

    @BeforeEach
    void startServer() {
        crossref = new WireMockServer(options().dynamicPort());
        crossref.start();
        resolver = new CrossrefResolver(crossref.baseUrl());
    }

    @AfterEach
    void stopServer() {
        crossref.stop();
    }

    @Test
    void supportsOnlyDoiLinks() {
        assertThat(resolver.supports(SPRINGER_URL)).isTrue();
        assertThat(resolver.supports("https://arxiv.org/abs/1706.03762")).isFalse();
    }

    @Test
    void knownDoiGivesRegistryData() {
        crossref.stubFor(get(urlEqualTo(SPRINGER_PATH)).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(Fixtures.read("crossref/springer-imagenet.json"))));

        ResolvedMetadata paper = resolver.resolve(SPRINGER_URL).orElseThrow();

        assertThat(paper.title()).isEqualTo("ImageNet Large Scale Visual Recognition Challenge");
        assertThat(paper.authors()).startsWith("Olga Russakovsky", "Jia Deng");
        assertThat(paper.publishedOn()).isEqualTo(LocalDate.of(2015, 4, 11));
        assertThat(paper.doi()).isEqualTo("10.1007/s11263-015-0816-y");
    }

    @Test
    void unknownDoiGivesEmpty() {
        crossref.stubFor(get(urlEqualTo("/works/10.1007%2Fnope")).willReturn(aResponse()
                .withStatus(404)
                .withBody("Resource not found.")));

        assertThat(resolver.resolve("https://doi.org/10.1007/nope")).isEqualTo(Optional.empty());
    }

    @Test
    void organisationAuthorAndYearOnlyDateAreHandled() {
        crossref.stubFor(get(urlEqualTo(SPRINGER_PATH)).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("""
                        {"status":"ok","message":{
                          "DOI":"10.1007/s11263-015-0816-y",
                          "title":["A report"],
                          "author":[{"name":"The Raman Lab"}],
                          "issued":{"date-parts":[[2020]]}}}""")));

        ResolvedMetadata paper = resolver.resolve(SPRINGER_URL).orElseThrow();

        assertThat(paper.authors()).containsExactly("The Raman Lab");
        assertThat(paper.publishedOn()).isEqualTo(LocalDate.of(2020, 1, 1));
    }

    @Test
    void recordWithoutTitleGivesEmpty() {
        crossref.stubFor(get(urlEqualTo(SPRINGER_PATH)).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("{\"status\":\"ok\",\"message\":{\"DOI\":\"10.1007/s11263-015-0816-y\"}}")));

        assertThat(resolver.resolve(SPRINGER_URL)).isEmpty();
    }

    @Test
    void serverErrorIsThrownSoTheCallerCanRetry() {
        crossref.stubFor(get(urlEqualTo(SPRINGER_PATH)).willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> resolver.resolve(SPRINGER_URL)).isInstanceOf(RuntimeException.class);
    }
}

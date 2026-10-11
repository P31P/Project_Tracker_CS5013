package tracker.papers.resolver;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tracker.data.PaperSource;
import tracker.support.Fixtures;

class ArxivResolverTest {

    private WireMockServer arxiv;
    private ArxivResolver resolver;

    @BeforeEach
    void startServer() {
        arxiv = new WireMockServer(options().dynamicPort());
        arxiv.start();
        resolver = new ArxivResolver(arxiv.baseUrl());
    }

    @AfterEach
    void stopServer() {
        arxiv.stop();
    }

    @Test
    void supportsOnlyArxivLinks() {
        assertThat(resolver.supports("https://arxiv.org/abs/1706.03762")).isTrue();
        assertThat(resolver.supports("https://doi.org/10.1007/s11263-015-0816-y")).isFalse();
    }

    @Test
    void knownIdGivesRegistryData() {
        stub("1706.03762", "arxiv/attention.xml");

        ResolvedMetadata paper = resolver.resolve("https://arxiv.org/abs/1706.03762").orElseThrow();

        assertThat(paper.title()).isEqualTo("Attention Is All You Need");
        assertThat(paper.authors()).hasSize(8).startsWith("Ashish Vaswani", "Noam Shazeer");
        assertThat(paper.publishedOn()).isEqualTo(LocalDate.of(2017, 6, 12));
        assertThat(paper.doi()).isNull();
        assertThat(paper.source()).isEqualTo(PaperSource.REGISTRY);
    }

    @Test
    void unknownIdGivesEmpty() {
        stub("9999.99999", "arxiv/unknown-id.xml");

        assertThat(resolver.resolve("https://arxiv.org/abs/9999.99999")).isEmpty();
    }

    @Test
    void malformedIdGivesEmpty() {
        stub("not-an-id", "arxiv/malformed-id.xml");

        assertThat(resolver.resolve("https://arxiv.org/abs/not-an-id")).isEmpty();
    }

    @Test
    void serverErrorIsThrownSoTheCallerCanRetry() {
        arxiv.stubFor(get(urlEqualTo("/api/query?id_list=1706.03762")).willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> resolver.resolve("https://arxiv.org/abs/1706.03762"))
                .isInstanceOf(RuntimeException.class);
    }

    private void stub(String id, String fixture) {
        arxiv.stubFor(get(urlEqualTo("/api/query?id_list=" + id)).willReturn(aResponse()
                .withHeader("Content-Type", "application/atom+xml")
                .withBody(Fixtures.read(fixture))));
    }
}

package tracker.papers.resolver;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tracker.data.PaperSource;
import tracker.support.Fixtures;

class MetaTagResolverTest {

    private WireMockServer publisher;
    private CrossrefResolver crossref;
    private MetaTagResolver resolver;
    private String articleUrl;

    @BeforeEach
    void setUp() {
        publisher = new WireMockServer(options().dynamicPort());
        publisher.start();
        crossref = mock(CrossrefResolver.class);
        when(crossref.resolve(anyString())).thenReturn(Optional.empty());
        resolver = new MetaTagResolver(crossref);
        articleUrl = publisher.baseUrl() + "/article";
    }

    @AfterEach
    void stopServer() {
        publisher.stop();
    }

    @Test
    void readsCitationTagsFromAPublisherPage() {
        servePage("html/nature-article.html");

        ResolvedMetadata paper = resolver.resolve(articleUrl).orElseThrow();

        assertThat(paper.title()).isEqualTo("Deep learning");
        assertThat(paper.authors()).containsExactly("Yann LeCun", "Yoshua Bengio", "Geoffrey Hinton");
        assertThat(paper.publishedOn()).isEqualTo(LocalDate.of(2015, 5, 1));
        assertThat(paper.doi()).isEqualTo("10.1038/nature14539");
        assertThat(paper.source()).isEqualTo(PaperSource.META_TAGS);
    }

    @Test
    void dateWrittenWithAMonthNameKeepsMonthAndDay() {
        servePage("html/pmc-article.html");

        ResolvedMetadata paper = resolver.resolve(articleUrl).orElseThrow();

        assertThat(paper.publishedOn()).isEqualTo(LocalDate.of(2019, 5, 3));
    }

    @Test
    void doiOnThePageIsLookedUpInCrossrefAndTheRegistryDataWins() {
        servePage("html/springer-article.html");
        ResolvedMetadata registry = new ResolvedMetadata("Registry title", List.of("A B"),
                LocalDate.of(2015, 4, 11), "10.1007/s11263-015-0816-y", PaperSource.REGISTRY);
        when(crossref.resolve("https://doi.org/10.1007/s11263-015-0816-y")).thenReturn(Optional.of(registry));

        assertThat(resolver.resolve(articleUrl)).contains(registry);
    }

    @Test
    void pageWithoutCitationTitleGivesEmpty() {
        publisher.stubFor(get(urlEqualTo("/article")).willReturn(aResponse()
                .withHeader("Content-Type", "text/html")
                .withBody("<html><head><title>Login</title></head><body>Sign in</body></html>")));

        assertThat(resolver.resolve(articleUrl)).isEmpty();
    }

    @Test
    void barePdfGivesEmpty() {
        publisher.stubFor(get(urlEqualTo("/article")).willReturn(aResponse()
                .withHeader("Content-Type", "application/pdf").withBody("%PDF-1.7")));

        assertThat(resolver.resolve(articleUrl)).isEmpty();
    }

    @Test
    void refusedOrMissingPageGivesEmpty() {
        publisher.stubFor(get(urlEqualTo("/article")).willReturn(aResponse().withStatus(403)));

        assertThat(resolver.resolve(articleUrl)).isEmpty();
    }

    @Test
    void serverErrorIsThrownSoTheCallerCanRetry() {
        publisher.stubFor(get(urlEqualTo("/article")).willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> resolver.resolve(articleUrl)).isInstanceOf(RuntimeException.class);
    }

    private void servePage(String fixture) {
        publisher.stubFor(get(urlEqualTo("/article")).willReturn(aResponse()
                .withHeader("Content-Type", "text/html; charset=utf-8")
                .withBody(Fixtures.read(fixture))));
    }
}

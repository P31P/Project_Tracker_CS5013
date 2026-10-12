package tracker.papers.resolver;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Builds the HTTP clients the resolvers use. */
final class ResolverHttp {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private ResolverHttp() {
    }

    /**
     * Creates a client with a 10-second connect and read timeout that follows redirects and names the app in its
     * User-Agent.
     *
     * @param baseUrl the service's base address, e.g. {@code https://api.crossref.org}
     * @return the client
     */
    static RestClient client(String baseUrl) {
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(http);
        requestFactory.setReadTimeout(TIMEOUT);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.USER_AGENT, "RamanLabTracker/0.1")
                .build();
    }
}

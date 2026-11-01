package tracker.llm;

import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class LlmClientTest {

    private static final Map<String, Object> SCHEMA = Map.of("type", "object",
            "properties", Map.of("title", Map.of("type", "string")));

    private WireMockServer model;

    @BeforeEach
    void startModel() {
        model = new WireMockServer(options().dynamicPort());
        model.start();
    }

    @AfterEach
    void stopModel() {
        model.stop();
    }

    private LlmClient client(boolean enabled, String apiKey, Duration timeout) {
        return new LlmClient(enabled, model.baseUrl() + "/v1", "llama3.2:3b", apiKey, timeout);
    }

    /** A chat reply as an OpenAI-style API sends it, with {@code content} as the model's answer. */
    static String reply(String content) {
        return JsonMapper.builder().build().writeValueAsString(
                Map.of("choices", java.util.List.of(Map.of("message", Map.of("role", "assistant", "content", content)))));
    }

    @Test
    void sendsTheModelPromptAndSchemaAndReturnsTheAnswer() {
        model.stubFor(post(urlEqualTo("/v1/chat/completions")).willReturn(okJson(reply("{\"title\": \"Flux sampling\"}"))));

        String answer = client(true, "", Duration.ofSeconds(5)).completeJson("Suggest a title", SCHEMA);

        assertThat(answer).isEqualTo("{\"title\": \"Flux sampling\"}");
        model.verify(postRequestedFor(urlEqualTo("/v1/chat/completions"))
                .withRequestBody(matchingJsonPath("$.model", equalTo("llama3.2:3b")))
                .withRequestBody(matchingJsonPath("$.messages[0].content", equalTo("Suggest a title")))
                .withRequestBody(matchingJsonPath("$.response_format.json_schema.schema.properties.title.type", equalTo("string")))
                .withHeader("Authorization", absent()));
    }

    @Test
    void sendsTheApiKeyWhenOneIsSet() {
        model.stubFor(post(urlEqualTo("/v1/chat/completions")).willReturn(okJson(reply("{}"))));

        client(true, "test-key", Duration.ofSeconds(5)).completeJson("Hi", SCHEMA);

        model.verify(postRequestedFor(urlEqualTo("/v1/chat/completions")).withHeader("Authorization", equalTo("Bearer test-key")));
    }

    @Test
    void serverErrorIsReportedAsLlmException() {
        model.stubFor(post(urlEqualTo("/v1/chat/completions")).willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> client(true, "", Duration.ofSeconds(5)).completeJson("Hi", SCHEMA))
                .isInstanceOf(LlmException.class);
    }

    @Test
    void slowModelTimesOut() {
        model.stubFor(post(urlEqualTo("/v1/chat/completions")).willReturn(okJson(reply("{}")).withFixedDelay(2000)));

        assertThatThrownBy(() -> client(true, "", Duration.ofMillis(300)).completeJson("Hi", SCHEMA))
                .isInstanceOf(LlmException.class);
    }

    @Test
    void replyWithoutAnAnswerIsReportedAsLlmException() {
        model.stubFor(post(urlEqualTo("/v1/chat/completions")).willReturn(okJson("{\"choices\": []}")));

        assertThatThrownBy(() -> client(true, "", Duration.ofSeconds(5)).completeJson("Hi", SCHEMA))
                .isInstanceOf(LlmException.class)
                .hasMessage("The language model sent no answer");
    }

    @Test
    void turnedOffClientNeverCallsTheModel() {
        assertThatThrownBy(() -> client(false, "", Duration.ofSeconds(5)).completeJson("Hi", SCHEMA))
                .isInstanceOf(LlmException.class)
                .hasMessage("The language model is turned off");
        assertThat(model.getAllServeEvents()).isEmpty();
    }
}

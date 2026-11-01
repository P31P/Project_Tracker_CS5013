package tracker.llm;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Asks a language model a question over the OpenAI-style chat API, which a local Ollama and hosted services such as
 * Groq and Gemini all accept, so switching between them is only a change of settings ({@code llm.*}).
 */
@Component
public class LlmClient {

    private final boolean enabled;
    private final String model;
    private final String apiKey;
    private final RestClient http;
    private final JsonMapper json = JsonMapper.builder().build();

    /**
     * Creates the client.
     *
     * @param enabled whether a model is available at all; when {@code false} every call fails fast
     * @param baseUrl the API's address up to {@code /chat/completions}, e.g. {@code http://localhost:11434/v1}
     * @param model the model to ask, e.g. {@code llama3.2:3b}
     * @param apiKey the API key; blank for a local model that needs none
     * @param timeout how long to wait for an answer
     */
    public LlmClient(@Value("${llm.enabled}") boolean enabled, @Value("${llm.base-url}") String baseUrl,
                     @Value("${llm.model}") String model, @Value("${llm.api-key}") String apiKey,
                     @Value("${llm.timeout}") Duration timeout) {
        this.enabled = enabled;
        this.model = model;
        this.apiKey = apiKey;
        JdkClientHttpRequestFactory requests = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        requests.setReadTimeout(timeout);
        this.http = RestClient.builder().baseUrl(baseUrl).requestFactory(requests).build();
    }

    /**
     * Says whether a model is configured.
     *
     * @return {@code true} if calls will be sent to the model
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Asks the model and makes it answer in the shape of a JSON schema. The answer still has to be checked by the
     * caller: a small model can follow the shape and still leave fields empty or wrong.
     *
     * @param prompt the question, including any text to work on
     * @param schema the JSON schema the answer must follow, as nested maps and lists
     * @return the model's answer: JSON text in the shape of the schema
     * @throws LlmException if the model is turned off, can't be reached, is too slow, or gives no answer
     */
    public String completeJson(String prompt, Map<String, Object> schema) {
        if (!enabled) {
            throw new LlmException("The language model is turned off");
        }
        Map<String, Object> request = Map.of(
                "model", model,
                "temperature", 0,
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "response_format", Map.of("type", "json_schema",
                        "json_schema", Map.of("name", "answer", "strict", true, "schema", schema)));
        try {
            String reply = http.post().uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> {
                        if (!apiKey.isBlank()) {
                            headers.setBearerAuth(apiKey);
                        }
                    })
                    .body(json.writeValueAsString(request))
                    .retrieve()
                    .body(String.class);
            JsonNode content = json.readTree(reply).path("choices").path(0).path("message").path("content");
            if (!content.isString()) {
                throw new LlmException("The language model sent no answer");
            }
            return content.asString();
        } catch (RestClientException | JacksonException e) {
            throw new LlmException("Could not get an answer from the language model", e);
        }
    }
}

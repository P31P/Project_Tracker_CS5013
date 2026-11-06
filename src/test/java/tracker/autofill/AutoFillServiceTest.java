package tracker.autofill;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;
import tracker.llm.LlmClient;

// the model is a WireMock stand-in that answers the way Ollama's OpenAI-style API does
class AutoFillServiceTest {

    private static final String COULD_NOT_SUGGEST = "Couldn't suggest anything this time. Fill in the form yourself, or try again.";

    private WireMockServer model;
    private AutoFillService autoFill;

    @BeforeEach
    void startModel() {
        model = new WireMockServer(options().dynamicPort());
        model.start();
        autoFill = service(true, Duration.ofSeconds(5));
    }

    @AfterEach
    void stopModel() {
        model.stop();
    }

    private AutoFillService service(boolean enabled, Duration timeout) {
        return new AutoFillService(new LlmClient(enabled, model.baseUrl() + "/v1", "llama3.2:3b", "", timeout),
                () -> List.of("Genomics", "Machine Learning"));
    }

    private void modelAnswers(String answer) {
        String reply = JsonMapper.builder().build().writeValueAsString(
                Map.of("choices", List.of(Map.of("message", Map.of("role", "assistant", "content", answer)))));
        model.stubFor(post(urlEqualTo("/v1/chat/completions")).willReturn(okJson(reply)));
    }

    private static void assertStatus(Throwable thrown, int status, String reason) {
        assertThat(thrown).isInstanceOf(ResponseStatusException.class);
        ResponseStatusException e = (ResponseStatusException) thrown;
        assertThat(e.getStatusCode().value()).isEqualTo(status);
        assertThat(e.getReason()).isEqualTo(reason);
    }

    @Test
    void goodAnswerIsTidied() {
        modelAnswers("{\"title\": \"  Flux sampling benchmark \", \"description\": \"Compare OPTGP and ACHR on 12 models.\","
                + " \"tags\": [\"#Metabolism\", \"metabolism\", \"GEM\", \"sampling\", \"ismb\", \"benchmark\", \"extra\"],"
                + " \"confidence\": 0.9}");

        AutoFillSuggestion suggestion = autoFill.suggest("flux sampling w/ OPTGP vs ACHR on 12 GEMs");

        assertThat(suggestion.title()).isEqualTo("Flux sampling benchmark");
        assertThat(suggestion.description()).isEqualTo("Compare OPTGP and ACHR on 12 models.");
        assertThat(suggestion.tags()).containsExactly("metabolism", "gem", "sampling", "ismb", "benchmark");
    }

    @Test
    void unicodeNoteReachesTheModelIntact() {
        modelAnswers("{\"title\": \"Slack threads as updates\", \"description\": \"\", \"tags\": []}");

        autoFill.suggest("idée :: use slack threads → project updates?? ¯\\_(ツ)_/¯");

        model.verify(postRequestedFor(urlEqualTo("/v1/chat/completions"))
                .withRequestBody(containing("idée"))
                .withRequestBody(containing("→ project updates"))
                .withRequestBody(containing("ツ")));
    }

    @Test
    void titleAloneIsEnough() {
        modelAnswers("{\"title\": \"Gut microbiome\"}");

        AutoFillSuggestion suggestion = autoFill.suggest("gut microbiome");

        assertThat(suggestion.title()).isEqualTo("Gut microbiome");
        assertThat(suggestion.description()).isNull();
        assertThat(suggestion.tags()).isEmpty();
    }

    @Test
    void longTitleIsCut() {
        modelAnswers("{\"title\": \"" + "a".repeat(300) + "\", \"description\": \"x\", \"tags\": []}");

        assertThat(autoFill.suggest("a long note").title()).hasSize(AutoFillService.MAX_TITLE_LENGTH);
    }

    @Test
    void fieldsOfTheWrongTypeCountAsMissing() {
        modelAnswers("{\"title\": \"Weekly paper club\", \"description\": 42, \"tags\": \"reading, papers\"}");

        AutoFillSuggestion suggestion = autoFill.suggest("paper club");

        assertThat(suggestion.description()).isNull();
        assertThat(suggestion.tags()).isEmpty();
    }

    @Test
    void answerWithoutATitleCouldNotSuggest() {
        modelAnswers("{\"title\": \"   \", \"description\": \"Something\", \"tags\": []}");

        assertStatus(catchThrowable(() -> autoFill.suggest("hmm")), 503, COULD_NOT_SUGGEST);
    }

    @Test
    void answerThatIsNotJsonCouldNotSuggest() {
        modelAnswers("Sure! Here is a title: Flux sampling benchmark");

        assertStatus(catchThrowable(() -> autoFill.suggest("flux sampling")), 503, COULD_NOT_SUGGEST);
    }

    @Test
    void modelThatIsDownCouldNotSuggest() {
        model.stubFor(post(urlEqualTo("/v1/chat/completions")).willReturn(aResponse().withStatus(500)));

        assertStatus(catchThrowable(() -> autoFill.suggest("flux sampling")), 503, COULD_NOT_SUGGEST);
    }

    @Test
    void slowModelCouldNotSuggest() {
        model.stubFor(post(urlEqualTo("/v1/chat/completions")).willReturn(okJson("{}").withFixedDelay(2000)));

        assertStatus(catchThrowable(() -> service(true, Duration.ofMillis(300)).suggest("flux sampling")), 503,
                COULD_NOT_SUGGEST);
    }

    @Test
    void blankOrTooLongNoteIsRefusedWithoutAskingTheModel() {
        assertStatus(catchThrowable(() -> autoFill.suggest("   ")), 400, "Write a few words first, then ask for a suggestion");
        assertStatus(catchThrowable(() -> autoFill.suggest("x".repeat(AutoFillService.MAX_NOTE_LENGTH + 1))), 400,
                "Keep the note under 4000 characters for a suggestion");
        assertThat(model.getAllServeEvents()).isEmpty();
    }

    @Test
    void turnedOffAutoFillSaysSo() {
        AutoFillService off = service(false, Duration.ofSeconds(5));

        assertThat(off.isEnabled()).isFalse();
        assertStatus(catchThrowable(() -> off.suggest("flux sampling")), 503, "Auto-fill is turned off");
    }

    @Test
    void theWriteUpIsFilledFromAFullNote() {
        modelAnswers("{\"title\": \"GNN flux prediction\", \"description\": \"Predict fluxes with a GNN.\","
                + " \"tags\": [\"Machine Learning\", \"genomics\"], \"coreQuestion\": \"Can a GNN predict flux?\","
                + " \"motivation\": \" Sampling is slow \", \"approach\": \"Train on BiGG\", \"resources\": \"GPUs\","
                + " \"challenges\": \"Little data\"}");

        AutoFillSuggestion suggestion = autoFill.suggest("gnn to predict flux, sampling is slow, train on bigg");

        assertThat(suggestion.coreQuestion()).isEqualTo("Can a GNN predict flux?");
        assertThat(suggestion.motivation()).isEqualTo("Sampling is slow");
        assertThat(suggestion.challenges()).isEqualTo("Little data");
        // a built-in tag comes back stored the way every tag is, which is the built-in tag's name
        assertThat(suggestion.tags()).containsExactly("machine learning", "genomics");
    }

    @Test
    void partsTheNoteDoesNotMentionStayEmpty() {
        modelAnswers("{\"title\": \"Gut microbiome\", \"description\": \"\", \"tags\": [],"
                + " \"coreQuestion\": \"\", \"motivation\": \"  \", \"approach\": 7, \"resources\": \"None mentioned\","
                + " \"challenges\": \"N/A.\"}");

        AutoFillSuggestion suggestion = autoFill.suggest("gut microbiome");

        assertThat(suggestion.coreQuestion()).isNull();
        assertThat(suggestion.motivation()).isNull();
        assertThat(suggestion.approach()).isNull();
        assertThat(suggestion.resources()).isNull();
        assertThat(suggestion.challenges()).isNull();
    }

    @Test
    void aVeryLongPartIsCut() {
        modelAnswers("{\"title\": \"Long\", \"approach\": \"" + "a".repeat(5000) + "\"}");

        assertThat(autoFill.suggest("long").approach()).hasSize(AutoFillService.MAX_PART_LENGTH);
    }

    @Test
    void theModelIsOfferedTheLabsTags() {
        modelAnswers("{\"title\": \"Anything\"}");

        autoFill.suggest("a note");

        model.verify(postRequestedFor(urlEqualTo("/v1/chat/completions"))
                .withRequestBody(containing("Genomics, Machine Learning")));
    }
}

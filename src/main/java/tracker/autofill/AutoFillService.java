package tracker.autofill;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tracker.llm.LlmClient;
import tracker.llm.LlmException;
import tracker.projects.ProjectService;

/**
 * Turns a rough note into a suggested title, description and tags with the language model. The model's answer is
 * checked, not trusted: anything unusable gives "couldn't suggest", never an error page, and nothing is saved.
 */
@Service
public class AutoFillService {

    /** Longest note accepted, so one request can't keep the model busy for minutes. */
    static final int MAX_NOTE_LENGTH = 4000;
    static final int MAX_TITLE_LENGTH = 120;
    static final int MAX_TAGS = 5;

    private static final Map<String, Object> SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "title", Map.of("type", "string"),
                    "description", Map.of("type", "string"),
                    "tags", Map.of("type", "array", "items", Map.of("type", "string"))),
            "required", List.of("title", "description", "tags"),
            "additionalProperties", false);

    private static final Logger log = LoggerFactory.getLogger(AutoFillService.class);
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final LlmClient llm;
    private final String prompt;

    /**
     * Creates the service and reads the prompt from {@code prompts/autofill.txt}.
     *
     * @param llm asks the language model
     */
    public AutoFillService(LlmClient llm) {
        this.llm = llm;
        try (InputStream in = new ClassPathResource("prompts/autofill.txt").getInputStream()) {
            this.prompt = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read prompts/autofill.txt", e);
        }
    }

    /**
     * Says whether suggestions are available, so the page can hide the Suggest button when they aren't.
     *
     * @return {@code true} if a language model is configured
     */
    public boolean isEnabled() {
        return llm.isEnabled();
    }

    /**
     * Suggests a title, description and tags for a note.
     *
     * @param note what the person wrote, however rough
     * @return the suggestion, for the person to check before saving
     * @throws ResponseStatusException {@code 400} if the note is blank or too long, or {@code 503} if auto-fill is off
     *         or the model gave nothing usable
     */
    public AutoFillSuggestion suggest(String note) {
        if (!llm.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Auto-fill is turned off");
        }
        if (note == null || note.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Write a few words first, then ask for a suggestion");
        }
        if (note.length() > MAX_NOTE_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Keep the note under " + MAX_NOTE_LENGTH + " characters for a suggestion");
        }
        String answer;
        try {
            answer = llm.completeJson(prompt + note.strip(), SCHEMA);
        } catch (LlmException e) {
            log.warn("Auto-fill failed: {}", e.getMessage());
            throw couldNotSuggest();
        }
        return read(answer);
    }

    /**
     * Reads the model's answer into a suggestion, tidying it on the way. Fields of the wrong type count as missing,
     * and fields the schema doesn't name are ignored.
     *
     * @param answer the model's answer, expected to be JSON with a title, description and tags
     * @return the tidied suggestion
     * @throws ResponseStatusException {@code 503} if the answer isn't JSON or has no title
     */
    static AutoFillSuggestion read(String answer) {
        JsonNode node;
        try {
            node = JSON.readTree(answer);
        } catch (JacksonException e) {
            throw couldNotSuggest();
        }
        String title = text(node.path("title"));
        if (title == null) {
            throw couldNotSuggest();
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            title = title.substring(0, MAX_TITLE_LENGTH).strip();
        }
        List<String> tags = new ArrayList<>();
        for (JsonNode tag : node.path("tags")) {
            if (tag.isString()) {
                tags.add(tag.asString());
            }
        }
        List<String> tidied = ProjectService.tags(tags);
        return new AutoFillSuggestion(title, text(node.path("description")),
                tidied.subList(0, Math.min(MAX_TAGS, tidied.size())));
    }

    // a string field's trimmed text, or null if it is missing, not a string, or blank
    private static String text(JsonNode field) {
        if (!field.isString() || field.asString().isBlank()) {
            return null;
        }
        return field.asString().strip();
    }

    private static ResponseStatusException couldNotSuggest() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Couldn't suggest anything this time. Fill in the form yourself, or try again.");
    }
}

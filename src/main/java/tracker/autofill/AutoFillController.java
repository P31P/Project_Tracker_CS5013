package tracker.autofill;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The auto-fill API under {@code /api/autofill}. It only suggests; saving stays with the projects and ideas APIs.
 */
@RestController
@RequestMapping("/api/autofill")
public class AutoFillController {

    /**
     * Whether auto-fill can be used.
     *
     * @param enabled {@code true} if a language model is configured
     */
    public record Status(boolean enabled) {
    }

    /**
     * A note to turn into a suggestion.
     *
     * @param note what the person wrote, however rough
     */
    public record Request(String note) {
    }

    private final AutoFillService autoFill;

    /**
     * Creates the controller.
     *
     * @param autoFill makes the suggestions
     */
    public AutoFillController(AutoFillService autoFill) {
        this.autoFill = autoFill;
    }

    /**
     * Says whether auto-fill is on, so the page knows whether to show the Suggest button. Handles
     * {@code GET /api/autofill}.
     *
     * @return whether auto-fill can be used
     */
    @GetMapping
    public Status status() {
        return new Status(autoFill.isEnabled());
    }

    /**
     * Suggests a title, description and tags for a note, without saving anything. Handles {@code POST /api/autofill}.
     *
     * @param request the note
     * @return the suggestion
     */
    @PostMapping
    public AutoFillSuggestion suggest(@RequestBody Request request) {
        return autoFill.suggest(request.note());
    }
}

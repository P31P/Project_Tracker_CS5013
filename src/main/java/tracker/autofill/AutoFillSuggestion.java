package tracker.autofill;

import java.util.List;

/**
 * Fields suggested for a new project or idea. Nothing is saved: the person checks and edits them first.
 *
 * @param title a short title
 * @param description one or two sentences, or {@code null} if the model gave none
 * @param tags up to five tags, tidied like any other tags
 */
public record AutoFillSuggestion(String title, String description, List<String> tags) {
}

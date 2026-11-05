package tracker.autofill;

import java.util.List;

/**
 * Fields suggested for a new project or idea. Nothing is saved: the person checks and edits them first. The five
 * parts of the longer write-up are {@code null} when the note says nothing about them.
 *
 * @param title a short title
 * @param description one or two sentences, or {@code null} if the model gave none
 * @param tags up to five tags, tidied like any other tags
 * @param coreQuestion the question the work tries to answer, or {@code null}
 * @param motivation why it matters, or {@code null}
 * @param approach how it could be done, or {@code null}
 * @param resources the data and resources it needs, or {@code null}
 * @param challenges what could make it hard, or {@code null}
 */
public record AutoFillSuggestion(String title, String description, List<String> tags, String coreQuestion,
                                 String motivation, String approach, String resources, String challenges) {
}

package tracker.profile;

import java.util.List;

/**
 * A person's changes to their own profile, sent to {@code PATCH /api/profile}. All three fields replace what is
 * stored, so the form sends them all.
 *
 * @param bio the introduction, in Markdown; blank or {@code null} for none
 * @param background the study background; blank or {@code null} for none
 * @param interests fields of interest, tidied like tags; {@code null} means none
 */
public record ProfileEdit(String bio, String background, List<String> interests) {
}

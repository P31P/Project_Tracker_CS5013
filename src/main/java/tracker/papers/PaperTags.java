package tracker.papers;

import java.util.List;

/**
 * A paper's new tags, sent to {@code PUT /api/papers/{id}/tags}. They replace the stored ones.
 *
 * @param tags the tags; tidied like project tags, and {@code null} means none
 */
public record PaperTags(List<String> tags) {
}

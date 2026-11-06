package tracker.profile;

import java.time.OffsetDateTime;
import tracker.data.Idea;

/**
 * A short line about one of a person's ideas, for their Home page and their page in the People tab.
 *
 * @param id the idea's id
 * @param title the title
 * @param voteCount how many people upvoted it
 * @param projectId the project it became, or {@code null}
 * @param createdAt when it was shared
 */
public record IdeaSummary(Long id, String title, long voteCount, Long projectId, OffsetDateTime createdAt) {

    /**
     * Summarises an idea. Its voters and project must be loaded.
     *
     * @param idea the idea
     * @return the summary
     */
    public static IdeaSummary of(Idea idea) {
        return new IdeaSummary(idea.getId(), idea.getTitle(), idea.getVoters().size(),
                idea.getProject() == null ? null : idea.getProject().getId(), idea.getCreatedAt());
    }
}

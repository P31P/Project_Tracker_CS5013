package tracker.papers;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import tracker.data.Paper;
import tracker.data.PaperShare;
import tracker.data.PaperSource;
import tracker.data.PaperStatus;
import tracker.data.SlackMessage;

/**
 * A paper as returned by the papers API. The Slack fields describe the first time the paper was shared; they
 * are {@code null} if it has no share. The vote and read fields are as seen by the signed-in user.
 *
 * @param id the paper's id
 * @param normalizedUrl the paper's normalized address
 * @param status how far the lookup of its details got
 * @param title the title, or {@code null} until resolved
 * @param authors the authors, or {@code null} until resolved
 * @param publishedOn the publication date, or {@code null}
 * @param doi the DOI, or {@code null}
 * @param source where the details came from, or {@code null}
 * @param createdAt when the paper was first stored
 * @param sharedBy the Slack name of whoever first shared it, or {@code null}
 * @param sharedAt when it was first shared, or {@code null}
 * @param slackPermalink a link to that Slack message, or {@code null}
 * @param tags the paper's tags
 * @param voteCount how many people upvoted it
 * @param votedByMe whether the signed-in user upvoted it
 * @param readByMe whether the signed-in user marked it as read
 */
public record PaperDto(Long id, String normalizedUrl, PaperStatus status, String title, List<String> authors,
                       LocalDate publishedOn, String doi, PaperSource source, OffsetDateTime createdAt,
                       String sharedBy, OffsetDateTime sharedAt, String slackPermalink, List<String> tags,
                       long voteCount, boolean votedByMe, boolean readByMe) {

    /**
     * Builds the view of a paper.
     *
     * @param paper the paper
     * @param firstShare the first time it was shared, or {@code null}
     * @param marks its votes and the signed-in user's marks
     * @return the paper as the API returns it
     */
    static PaperDto from(Paper paper, PaperShare firstShare, PaperMarks.Marks marks) {
        SlackMessage message = firstShare == null ? null : firstShare.getSlackMessage();
        return new PaperDto(paper.getId(), paper.getNormalizedUrl(), paper.getStatus(), paper.getTitle(),
                paper.getAuthors(), paper.getPublishedOn(), paper.getDoi(), paper.getSource(),
                paper.getCreatedAt(),
                message == null ? null : message.getSlackUserName(),
                message == null ? null : message.getReceivedAt(),
                message == null ? null : message.getPermalink(),
                paper.getTags(), marks.voteCount(), marks.votedByMe(), marks.readByMe());
    }
}

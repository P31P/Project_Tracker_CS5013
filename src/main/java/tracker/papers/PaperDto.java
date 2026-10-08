package tracker.papers;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import tracker.data.Paper;
import tracker.data.PaperShare;
import tracker.data.PaperSource;
import tracker.data.PaperStatus;
import tracker.data.SlackMessage;

/** The Slack fields describe the first time the paper was shared; they are null if it has no share. */
public record PaperDto(Long id, String normalizedUrl, PaperStatus status, String title, List<String> authors,
                       LocalDate publishedOn, String doi, PaperSource source, OffsetDateTime createdAt,
                       String sharedBy, OffsetDateTime sharedAt, String slackPermalink) {

    static PaperDto from(Paper paper, PaperShare firstShare) {
        SlackMessage message = firstShare == null ? null : firstShare.getSlackMessage();
        return new PaperDto(paper.getId(), paper.getNormalizedUrl(), paper.getStatus(), paper.getTitle(),
                paper.getAuthors(), paper.getPublishedOn(), paper.getDoi(), paper.getSource(),
                paper.getCreatedAt(),
                message == null ? null : message.getSlackUserName(),
                message == null ? null : message.getReceivedAt(),
                message == null ? null : message.getPermalink());
    }
}

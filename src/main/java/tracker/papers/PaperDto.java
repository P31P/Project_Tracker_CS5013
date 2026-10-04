package tracker.papers;

import java.time.OffsetDateTime;
import tracker.data.Paper;
import tracker.data.PaperStatus;

public record PaperDto(Long id, String normalizedUrl, PaperStatus status, OffsetDateTime createdAt) {

    static PaperDto from(Paper paper) {
        return new PaperDto(paper.getId(), paper.getNormalizedUrl(),
                paper.getStatus(), paper.getCreatedAt());
    }
}

package tracker.papers;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import tracker.data.Paper;
import tracker.data.PaperSource;
import tracker.data.PaperStatus;

public record PaperDto(Long id, String normalizedUrl, PaperStatus status, String title, List<String> authors,
                       LocalDate publishedOn, String doi, PaperSource source, OffsetDateTime createdAt) {

    static PaperDto from(Paper paper) {
        return new PaperDto(paper.getId(), paper.getNormalizedUrl(), paper.getStatus(), paper.getTitle(),
                paper.getAuthors(), paper.getPublishedOn(), paper.getDoi(), paper.getSource(),
                paper.getCreatedAt());
    }
}

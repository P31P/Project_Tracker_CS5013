package tracker.papers.resolver;

import java.time.LocalDate;
import java.util.List;
import tracker.data.PaperSource;

/**
 * Paper details found by a resolver. Only the title is guaranteed.
 *
 * @param title the title
 * @param authors the authors in order; may be empty
 * @param publishedOn the publication date, or {@code null} if unknown
 * @param doi the DOI, or {@code null} if unknown
 * @param source where the details came from
 * @param references the paper's reference list, in order; empty if the source doesn't give one
 */
public record ResolvedMetadata(String title, List<String> authors, LocalDate publishedOn, String doi,
                               PaperSource source, List<ResolvedReference> references) {

    /**
     * Creates details without a reference list, for sources that never have one.
     *
     * @param title the title
     * @param authors the authors in order; may be empty
     * @param publishedOn the publication date, or {@code null} if unknown
     * @param doi the DOI, or {@code null} if unknown
     * @param source where the details came from
     */
    public ResolvedMetadata(String title, List<String> authors, LocalDate publishedOn, String doi,
                            PaperSource source) {
        this(title, authors, publishedOn, doi, source, List.of());
    }
}

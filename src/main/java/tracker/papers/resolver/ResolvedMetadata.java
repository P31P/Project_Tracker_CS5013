package tracker.papers.resolver;

import java.time.LocalDate;
import java.util.List;
import tracker.data.PaperSource;

/** Paper details found by a resolver. Only the title is guaranteed; the rest may be null or empty. */
public record ResolvedMetadata(String title, List<String> authors, LocalDate publishedOn, String doi,
                               PaperSource source) {
}

package tracker.papers;

import java.time.LocalDate;
import java.util.List;

/**
 * A person's corrections to a paper's details, sent to {@code PATCH /api/papers/{id}}. All four fields replace
 * what is stored, so the form sends them all.
 *
 * @param title the title; required
 * @param authors the authors in order; blank names are dropped, and {@code null} means none
 * @param publishedOn the publication date; may be {@code null}
 * @param doi the DOI; may be {@code null} or blank for none
 */
public record PaperEdit(String title, List<String> authors, LocalDate publishedOn, String doi) {
}

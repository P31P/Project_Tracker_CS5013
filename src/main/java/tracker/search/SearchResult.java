package tracker.search;

/**
 * One match from search.
 *
 * @param type what kind of thing matched
 * @param id the id of the matching project, idea or paper
 * @param title the title, or the paper's address if it has no title yet
 * @param detail the start of the description, or a paper's authors; {@code null} if there is none
 * @param rank how well it matches; higher is better
 */
public record SearchResult(SearchType type, long id, String title, String detail, double rank) {
}

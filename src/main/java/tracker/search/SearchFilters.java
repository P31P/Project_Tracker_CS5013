package tracker.search;

import java.time.LocalDate;
import java.util.List;

/**
 * Narrows a search. Every filter is optional; together they must all match.
 *
 * @param type only this kind of result; all kinds if {@code null}
 * @param tags results must have all of these tags (already tidied); none if empty
 * @param personId only projects this person owns or is a member of, and ideas they wrote; papers are left out,
 *                 since they aren't linked to app users. {@code null} for anyone
 * @param from only things added on or after this day; {@code null} for no start
 * @param to only things added on or before this day; {@code null} for no end
 */
public record SearchFilters(SearchType type, List<String> tags, Long personId, LocalDate from, LocalDate to) {

    /** No filters at all. */
    public static final SearchFilters NONE = new SearchFilters(null, List.of(), null, null, null);

    /**
     * Makes sure {@code tags} is never {@code null}.
     *
     * @param type only this kind of result, or {@code null}
     * @param tags the tags every result must have; {@code null} means none
     * @param personId the person, or {@code null}
     * @param from the first day, or {@code null}
     * @param to the last day, or {@code null}
     */
    public SearchFilters {
        tags = tags == null ? List.of() : List.copyOf(tags);
    }

    /**
     * Returns only a type filter.
     *
     * @param type the kind of result to keep; {@code null} for all
     * @return the filters
     */
    public static SearchFilters only(SearchType type) {
        return new SearchFilters(type, List.of(), null, null, null);
    }

    /**
     * Says whether any filter that can stand without a query is set: tags, a person or a date. The type alone
     * doesn't count, so an empty search box with only a type still finds nothing.
     *
     * @return {@code true} if tags, a person or a date is set
     */
    public boolean narrowsWithoutQuery() {
        return !tags.isEmpty() || personId != null || from != null || to != null;
    }
}

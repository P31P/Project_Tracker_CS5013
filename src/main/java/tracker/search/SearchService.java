package tracker.search;

import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/** Full-text search across projects, ideas and papers, with optional filters. */
@Service
public class SearchService {

    private static final int MAX_RESULTS = 50;
    private static final int DETAIL_LENGTH = 160;

    // Each branch only runs when the type filter is empty or names its table. Without search text, q.query is
    // NULL, every row passes the text test and ranks 0, so the newest come first. An empty tag array is
    // contained in every array, so "no tags" needs no special case.
    private static final String SQL = """
            WITH q AS (SELECT websearch_to_tsquery('english', :query) AS query)
            SELECT type, id, title, detail, rank, created_at, tags FROM (
                SELECT 'PROJECT' AS type, p.id, p.title, left(p.description, :detailLength) AS detail,
                       coalesce(ts_rank(p.search, q.query), 0) AS rank, p.created_at, p.tags
                FROM project p, q
                WHERE (q.query IS NULL OR p.search @@ q.query)
                  AND (CAST(:type AS text) IS NULL OR :type = 'PROJECT')
                  AND p.tags @> CAST(:tags AS text[])
                  AND (CAST(:person AS bigint) IS NULL OR p.owner_id = :person
                       OR EXISTS (SELECT 1 FROM project_member m WHERE m.project_id = p.id AND m.user_id = :person))
                  AND (CAST(:from AS date) IS NULL OR p.created_at >= CAST(:from AS date))
                  AND (CAST(:to AS date) IS NULL OR p.created_at < CAST(:to AS date) + 1)
                UNION ALL
                SELECT 'IDEA', i.id, i.title, left(i.description, :detailLength),
                       coalesce(ts_rank(i.search, q.query), 0), i.created_at, i.tags
                FROM idea i, q
                WHERE (q.query IS NULL OR i.search @@ q.query)
                  AND (CAST(:type AS text) IS NULL OR :type = 'IDEA')
                  AND i.tags @> CAST(:tags AS text[])
                  AND (CAST(:person AS bigint) IS NULL OR i.author_id = :person)
                  AND (CAST(:from AS date) IS NULL OR i.created_at >= CAST(:from AS date))
                  AND (CAST(:to AS date) IS NULL OR i.created_at < CAST(:to AS date) + 1)
                UNION ALL
                -- papers aren't linked to app users, so a person filter leaves them out
                SELECT 'PAPER', a.id, coalesce(a.title, a.normalized_url), array_to_string(a.authors, ', '),
                       coalesce(ts_rank(a.search, q.query), 0), a.created_at, a.tags
                FROM paper a, q
                WHERE (q.query IS NULL OR a.search @@ q.query)
                  AND (CAST(:type AS text) IS NULL OR :type = 'PAPER')
                  AND a.tags @> CAST(:tags AS text[])
                  AND CAST(:person AS bigint) IS NULL
                  AND (CAST(:from AS date) IS NULL OR a.created_at >= CAST(:from AS date))
                  AND (CAST(:to AS date) IS NULL OR a.created_at < CAST(:to AS date) + 1)
            ) matches
            ORDER BY rank DESC, created_at DESC, type, id
            LIMIT :limit
            """;

    private final JdbcClient jdbc;

    /**
     * Creates the service.
     *
     * @param jdbc runs the search query
     */
    public SearchService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Finds projects, ideas and papers that match a query and the filters, best match first.
     *
     * <p>The query uses web-search syntax: words are ANDed, {@code "quoted phrases"} match exactly,
     * {@code or} allows either word and a leading {@code -} excludes a word. With a blank query, everything
     * that passes the filters is listed, newest first, as long as tags, a person or a date is set.
     *
     * @param query the text to look for; may be {@code null} or blank
     * @param filters what to narrow the results to
     * @return up to 50 matches, or an empty list if there is neither a query nor a filter that narrows
     */
    public List<SearchResult> search(String query, SearchFilters filters) {
        String text = query == null || query.isBlank() ? null : query;
        if (text == null && !filters.narrowsWithoutQuery()) {
            return List.of();
        }
        SearchType type = filters.type();
        return jdbc.sql(SQL)
                .param("query", text, Types.VARCHAR)
                .param("type", type == null ? null : type.name(), Types.VARCHAR)
                .param("tags", filters.tags().toArray(String[]::new))
                .param("person", filters.personId(), Types.BIGINT)
                .param("from", filters.from(), Types.DATE)
                .param("to", filters.to(), Types.DATE)
                .param("detailLength", DETAIL_LENGTH)
                .param("limit", MAX_RESULTS)
                .query((rs, row) -> new SearchResult(SearchType.valueOf(rs.getString("type")), rs.getLong("id"),
                        rs.getString("title"), rs.getString("detail"), rs.getDouble("rank"),
                        rs.getObject("created_at", OffsetDateTime.class),
                        List.of((String[]) rs.getArray("tags").getArray())))
                .list();
    }
}

package tracker.search;

import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/** Full-text search across projects, ideas and papers. */
@Service
public class SearchService {

    private static final int MAX_RESULTS = 50;
    private static final int DETAIL_LENGTH = 160;

    // each branch only runs when the type filter is empty or names its table
    private static final String SQL = """
            WITH q AS (SELECT websearch_to_tsquery('english', :query) AS query)
            SELECT type, id, title, detail, rank FROM (
                SELECT 'PROJECT' AS type, p.id, p.title, left(p.description, :detailLength) AS detail,
                       ts_rank(p.search, q.query) AS rank
                FROM project p, q
                WHERE p.search @@ q.query AND (CAST(:type AS text) IS NULL OR :type = 'PROJECT')
                UNION ALL
                SELECT 'IDEA', i.id, i.title, left(i.description, :detailLength), ts_rank(i.search, q.query)
                FROM idea i, q
                WHERE i.search @@ q.query AND (CAST(:type AS text) IS NULL OR :type = 'IDEA')
                UNION ALL
                SELECT 'PAPER', a.id, coalesce(a.title, a.normalized_url), array_to_string(a.authors, ', '),
                       ts_rank(a.search, q.query)
                FROM paper a, q
                WHERE a.search @@ q.query AND (CAST(:type AS text) IS NULL OR :type = 'PAPER')
            ) matches
            ORDER BY rank DESC, type, id
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
     * Finds projects, ideas and papers that match a query, best match first.
     *
     * <p>The query uses web-search syntax: words are ANDed, {@code "quoted phrases"} match exactly,
     * {@code or} allows either word and a leading {@code -} excludes a word.
     *
     * @param query the text to look for; may be {@code null}
     * @param type only this kind of result; all kinds if {@code null}
     * @return up to 50 matches, or an empty list if the query is blank
     */
    public List<SearchResult> search(String query, SearchType type) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return jdbc.sql(SQL)
                .param("query", query)
                .param("type", type == null ? null : type.name(), java.sql.Types.VARCHAR)
                .param("detailLength", DETAIL_LENGTH)
                .param("limit", MAX_RESULTS)
                .query((rs, row) -> new SearchResult(SearchType.valueOf(rs.getString("type")), rs.getLong("id"),
                        rs.getString("title"), rs.getString("detail"), rs.getDouble("rank")))
                .list();
    }
}

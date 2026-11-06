package tracker.profile;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import tracker.data.AppUser;

/**
 * Finds new work that matches a person's interests, for "For you" on their Home page. Worked out each time Home
 * loads, so nothing is stored and there is no read/unread state.
 */
@Service
public class InterestMatches {

    private static final int DAYS = 30;
    private static final int LIMIT = 12;

    /**
     * One thing that matches an interest.
     *
     * @param kind {@code IDEA}, {@code PROJECT} or {@code PAPER}
     * @param who who shared it, or {@code null} for a paper from Slack
     * @param action what they did, e.g. "shared an idea"
     * @param target the title of the idea, project or paper
     * @param href a tab link such as {@code #papers/12}
     * @param interest the interest it matched, e.g. "genomics"
     * @param at when it was added
     */
    public record Match(String kind, String who, String action, String target, String href, String interest,
                        OffsetDateTime at) {
    }

    // An idea matches when one of its tags is an interest, and so does a project that was opened for
    // collaboration (by when it was opened) unless the person is already on it. A paper matches the same way, or
    // when an interest appears as a phrase in its title, so untagged papers from Slack show up too. Papers the
    // person has already read are left out.
    private static final String SQL = """
            SELECT kind, who, action, target, href, interest, at FROM (
                SELECT 'IDEA' AS kind, coalesce(u.name, u.email) AS who, 'shared an idea' AS action,
                       i.title AS target, '#ideas' AS href,
                       (SELECT t FROM unnest(i.tags) t WHERE t = ANY (CAST(:interests AS text[])) LIMIT 1) AS interest,
                       i.created_at AS at
                FROM idea i LEFT JOIN app_user u ON u.id = i.author_id
                WHERE i.created_at > now() - make_interval(days => :days)
                  AND i.author_id IS DISTINCT FROM :me
                UNION ALL
                SELECT 'PROJECT', coalesce(u.name, u.email), 'opened a project for collaboration', p.title,
                       '#projects/' || p.id,
                       (SELECT t FROM unnest(p.tags) t WHERE t = ANY (CAST(:interests AS text[])) LIMIT 1),
                       p.collab_opened_at
                FROM project p LEFT JOIN app_user u ON u.id = p.owner_id
                WHERE p.open_for_collab
                  AND p.collab_opened_at > now() - make_interval(days => :days)
                  AND p.owner_id IS DISTINCT FROM :me
                  AND NOT EXISTS (SELECT 1 FROM project_member m WHERE m.project_id = p.id AND m.user_id = :me)
                UNION ALL
                SELECT 'PAPER', NULL, 'A paper was shared in Slack', coalesce(a.title, a.normalized_url),
                       '#papers/' || a.id,
                       coalesce(
                           (SELECT t FROM unnest(a.tags) t WHERE t = ANY (CAST(:interests AS text[])) LIMIT 1),
                           (SELECT t FROM unnest(CAST(:interests AS text[])) t
                            WHERE to_tsvector('english', coalesce(a.title, '')) @@ phraseto_tsquery('english', t)
                            LIMIT 1)),
                       a.created_at
                FROM paper a
                WHERE a.created_at > now() - make_interval(days => :days)
                  AND NOT EXISTS (SELECT 1 FROM paper_read r WHERE r.paper_id = a.id AND r.user_id = :me)
            ) matches
            WHERE interest IS NOT NULL
            ORDER BY at DESC
            LIMIT :limit
            """;

    private final JdbcClient jdbc;

    /**
     * Creates the service.
     *
     * @param jdbc runs the query
     */
    public InterestMatches(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Finds ideas, projects open for collaboration and papers from the last 30 days that match a person's
     * interests, newest first. Their own ideas and projects don't count.
     *
     * @param user the person; may be {@code null}
     * @return up to 12 matches, or none if the person has no interests
     */
    public List<Match> forUser(AppUser user) {
        if (user == null || user.getInterests().isEmpty()) {
            return List.of();
        }
        return jdbc.sql(SQL)
                .param("interests", user.getInterests().toArray(String[]::new))
                .param("me", user.getId())
                .param("days", DAYS)
                .param("limit", LIMIT)
                .query((rs, row) -> new Match(rs.getString("kind"), rs.getString("who"), rs.getString("action"),
                        rs.getString("target"), rs.getString("href"), rs.getString("interest"),
                        rs.getObject("at", OffsetDateTime.class)))
                .list();
    }
}

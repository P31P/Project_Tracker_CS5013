package tracker.papers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import tracker.data.AppUser;

/** Upvotes on papers, and each person's private "read" marks. */
@Service
public class PaperMarks {

    /**
     * How one paper looks to one person.
     *
     * @param voteCount how many people upvoted it
     * @param votedByMe whether this person upvoted it
     * @param readByMe whether this person marked it as read
     */
    public record Marks(long voteCount, boolean votedByMe, boolean readByMe) {
    }

    /**
     * The votes on every paper and one person's own marks, loaded together for a list of papers.
     *
     * @param voteCounts votes per paper id; papers without votes are missing
     * @param voted the ids of the papers this person upvoted
     * @param read the ids of the papers this person marked as read
     */
    public record Snapshot(Map<Long, Long> voteCounts, Set<Long> voted, Set<Long> read) {

        /**
         * Returns how a paper looks to the person this snapshot was taken for.
         *
         * @param paperId the paper's id
         * @return its votes and the person's marks
         */
        public Marks of(Long paperId) {
            return new Marks(voteCounts.getOrDefault(paperId, 0L), voted.contains(paperId), read.contains(paperId));
        }
    }

    private final JdbcClient jdbc;

    /**
     * Creates the service.
     *
     * @param jdbc runs the queries
     */
    public PaperMarks(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Loads the vote counts of all papers and a person's own votes and read marks. Three small queries, however
     * many papers there are.
     *
     * @param viewer the person; may be {@code null}, which gives no votes or read marks of their own
     * @return the counts and marks
     */
    public Snapshot snapshot(AppUser viewer) {
        Map<Long, Long> counts = new HashMap<>();
        jdbc.sql("SELECT paper_id, count(*) AS votes FROM paper_vote GROUP BY paper_id")
                .query((RowCallbackHandler) rs -> counts.put(rs.getLong("paper_id"), rs.getLong("votes")));
        if (viewer == null) {
            return new Snapshot(counts, Set.of(), Set.of());
        }
        return new Snapshot(counts, idsFor("paper_vote", viewer), idsFor("paper_read", viewer));
    }

    /**
     * Records a person's upvote. Voting twice counts once.
     *
     * @param paperId the paper's id
     * @param voter who is voting; if {@code null}, nothing is recorded
     */
    public void vote(Long paperId, AppUser voter) {
        add("paper_vote", paperId, voter);
    }

    /**
     * Takes back a person's upvote. Does nothing if they hadn't voted.
     *
     * @param paperId the paper's id
     * @param voter whose vote to remove; if {@code null}, nothing changes
     */
    public void unvote(Long paperId, AppUser voter) {
        remove("paper_vote", paperId, voter);
    }

    /**
     * Marks a paper as read by a person. Marking it twice changes nothing.
     *
     * @param paperId the paper's id
     * @param reader who read it; if {@code null}, nothing is recorded
     */
    public void markRead(Long paperId, AppUser reader) {
        add("paper_read", paperId, reader);
    }

    /**
     * Marks a paper as not read yet by a person.
     *
     * @param paperId the paper's id
     * @param reader whose mark to remove; if {@code null}, nothing changes
     */
    public void markUnread(Long paperId, AppUser reader) {
        remove("paper_read", paperId, reader);
    }

    // table is always one of the two constants above, never user input
    private Set<Long> idsFor(String table, AppUser user) {
        return new HashSet<>(jdbc.sql("SELECT paper_id FROM " + table + " WHERE user_id = :user")
                .param("user", user.getId())
                .query(Long.class)
                .list());
    }

    private void add(String table, Long paperId, AppUser user) {
        if (user != null) {
            jdbc.sql("INSERT INTO " + table + " (paper_id, user_id) VALUES (:paper, :user) ON CONFLICT DO NOTHING")
                    .param("paper", paperId)
                    .param("user", user.getId())
                    .update();
        }
    }

    private void remove(String table, Long paperId, AppUser user) {
        if (user != null) {
            jdbc.sql("DELETE FROM " + table + " WHERE paper_id = :paper AND user_id = :user")
                    .param("paper", paperId)
                    .param("user", user.getId())
                    .update();
        }
    }
}

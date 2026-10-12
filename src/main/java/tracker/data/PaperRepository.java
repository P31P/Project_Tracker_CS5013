package tracker.data;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Stores and finds {@link Paper}s. */
public interface PaperRepository extends JpaRepository<Paper, Long> {

    /**
     * Finds a paper by its normalized address.
     *
     * @param normalizedUrl the address as produced by {@code LinkExtractor}
     * @return the paper, or empty if it was never shared
     */
    Optional<Paper> findByNormalizedUrl(String normalizedUrl);

    /**
     * Lists the papers with a status.
     *
     * @param status the status to filter by
     * @return the matching papers
     */
    List<Paper> findByStatus(PaperStatus status);

    /**
     * Lists the ten newest papers, for the activity feed.
     *
     * @return up to ten papers, newest first
     */
    List<Paper> findTop10ByOrderByCreatedAtDescIdDesc();

    /**
     * Claims the next pending paper that is due, locking its row until the transaction ends. Papers that failed
     * before go to the back, and {@code SKIP LOCKED} means two app instances never claim the same row.
     *
     * @return the claimed paper, or empty if none is due
     */
    @Query(value = "SELECT * FROM paper WHERE status = 'PENDING' "
            + "AND (next_attempt_at IS NULL OR next_attempt_at <= now()) "
            + "ORDER BY attempts, id LIMIT 1 FOR UPDATE SKIP LOCKED", nativeQuery = true)
    Optional<Paper> claimNextPending();
}

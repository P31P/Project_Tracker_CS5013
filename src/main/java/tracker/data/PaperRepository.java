package tracker.data;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PaperRepository extends JpaRepository<Paper, Long> {

    Optional<Paper> findByNormalizedUrl(String normalizedUrl);

    List<Paper> findByStatus(PaperStatus status);

    // papers that failed before go to the back; SKIP LOCKED lets two app instances never claim the same row
    @Query(value = "SELECT * FROM paper WHERE status = 'PENDING' ORDER BY attempts, id LIMIT 1 "
            + "FOR UPDATE SKIP LOCKED", nativeQuery = true)
    Optional<Paper> claimNextPending();
}

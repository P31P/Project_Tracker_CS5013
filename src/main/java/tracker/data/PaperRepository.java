package tracker.data;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaperRepository extends JpaRepository<Paper, Long> {

    Optional<Paper> findByNormalizedUrl(String normalizedUrl);

    List<Paper> findByStatus(PaperStatus status);
}

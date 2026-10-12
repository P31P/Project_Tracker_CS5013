package tracker.data;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectLinkRepository extends JpaRepository<ProjectLink, Long> {

    List<ProjectLink> findByProjectIdOrderById(Long projectId);

    Optional<ProjectLink> findByIdAndProjectId(Long id, Long projectId);
}

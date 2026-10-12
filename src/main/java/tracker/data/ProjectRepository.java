package tracker.data;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    // loads owners and members in the same query instead of one query per project
    @EntityGraph(attributePaths = {"owner", "members"})
    List<Project> findAllByOrderByUpdatedAtDescIdDesc();
}

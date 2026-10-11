package tracker.data;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProjectUpdateRepository extends JpaRepository<ProjectUpdate, Long> {

    List<ProjectUpdate> findByProjectIdOrderByCreatedAtDescIdDesc(Long projectId);

    // [project id, number of updates] for every project, so the list needs one query instead of one per project
    @Query("select u.project.id, count(u) from ProjectUpdate u group by u.project.id")
    List<Object[]> countPerProject();
}

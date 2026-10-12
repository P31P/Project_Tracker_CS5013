package tracker.data;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Stores and finds {@link ProjectUpdate}s. */
public interface ProjectUpdateRepository extends JpaRepository<ProjectUpdate, Long> {

    /**
     * Lists a project's timeline, newest first.
     *
     * @param projectId the project's id
     * @return the project's entries
     */
    List<ProjectUpdate> findByProjectIdOrderByCreatedAtDescIdDesc(Long projectId);

    /**
     * Lists the newest timeline entries across all projects, for the activity feed.
     *
     * @return up to fifteen entries, newest first
     */
    List<ProjectUpdate> findTop15ByOrderByCreatedAtDescIdDesc();

    /**
     * Counts the timeline entries of every project in one query, so the project list doesn't need one
     * query per project.
     *
     * @return one {@code [project id, number of entries]} pair per project that has entries
     */
    @Query("select u.project.id, count(u) from ProjectUpdate u group by u.project.id")
    List<Object[]> countPerProject();
}

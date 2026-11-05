package tracker.data;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Stores and finds {@link Project}s. */
public interface ProjectRepository extends JpaRepository<Project, Long> {

    /**
     * Lists every project, most recently changed first. Owners and members are loaded in the same query
     * instead of one query per project.
     *
     * @return all projects
     */
    @EntityGraph(attributePaths = {"owner", "members", "voters"})
    List<Project> findAllByOrderByUpdatedAtDescIdDesc();

    /**
     * Lists the ten newest projects, for the activity feed.
     *
     * @return up to ten projects, newest first
     */
    List<Project> findTop10ByOrderByCreatedAtDescIdDesc();

    /**
     * Counts the projects with a status.
     *
     * @param status the status to count
     * @return the number of projects with that status
     */
    long countByStatus(ProjectStatus status);
}

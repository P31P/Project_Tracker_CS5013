package tracker.data;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Stores and finds {@link ProjectLink}s. */
public interface ProjectLinkRepository extends JpaRepository<ProjectLink, Long> {

    /**
     * Lists a project's links in the order they were added.
     *
     * @param projectId the project's id
     * @return the project's links
     */
    List<ProjectLink> findByProjectIdOrderById(Long projectId);

    /**
     * Finds a link only if it belongs to the given project.
     *
     * @param id the link's id
     * @param projectId the project's id
     * @return the link, or empty if there is none with this id on this project
     */
    Optional<ProjectLink> findByIdAndProjectId(Long id, Long projectId);
}

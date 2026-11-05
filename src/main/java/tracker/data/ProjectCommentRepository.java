package tracker.data;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Stores and finds {@link ProjectComment}s. */
public interface ProjectCommentRepository extends JpaRepository<ProjectComment, Long> {

    /**
     * Lists a project's discussion, oldest first.
     *
     * @param projectId the project's id
     * @return its comments
     */
    List<ProjectComment> findByProjectIdOrderByCreatedAtAscIdAsc(Long projectId);

    /**
     * Counts the comments on one project.
     *
     * @param projectId the project's id
     * @return how many comments it has
     */
    long countByProjectId(Long projectId);

    /**
     * Counts the comments on every project that has any, in one query.
     *
     * @return pairs of project id and count
     */
    @Query("select c.project.id, count(c) from ProjectComment c group by c.project.id")
    List<Object[]> countPerProject();
}

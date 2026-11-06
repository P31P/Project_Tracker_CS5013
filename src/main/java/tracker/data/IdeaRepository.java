package tracker.data;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Stores and finds {@link Idea}s. */
public interface IdeaRepository extends JpaRepository<Idea, Long> {

    /**
     * Lists every idea, newest first, with authors, projects and voters loaded in the same query.
     *
     * @return all ideas
     */
    @EntityGraph(attributePaths = {"author", "project", "voters", "papers"})
    List<Idea> findAllByOrderByCreatedAtDescIdDesc();

    /**
     * Lists the ten newest ideas, for the activity feed.
     *
     * @return up to ten ideas, newest first
     */
    List<Idea> findTop10ByOrderByCreatedAtDescIdDesc();

    /**
     * Lists the ideas a person wrote, newest first, with voters and projects loaded in the same query.
     *
     * @param authorId the author's id
     * @return their ideas
     */
    @EntityGraph(attributePaths = {"project", "voters"})
    List<Idea> findByAuthorIdOrderByCreatedAtDescIdDesc(Long authorId);

    /**
     * Counts the ideas that have not become projects.
     *
     * @return the number of open ideas
     */
    long countByProjectIsNull();
}

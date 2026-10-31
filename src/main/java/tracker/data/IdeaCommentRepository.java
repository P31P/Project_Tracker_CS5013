package tracker.data;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Stores and finds {@link IdeaComment}s. */
public interface IdeaCommentRepository extends JpaRepository<IdeaComment, Long> {

    /**
     * Lists an idea's discussion, oldest first, so it reads top to bottom.
     *
     * @param ideaId the idea's id
     * @return the idea's comments
     */
    List<IdeaComment> findByIdeaIdOrderByCreatedAtAscIdAsc(Long ideaId);

    /**
     * Counts one idea's comments.
     *
     * @param ideaId the idea's id
     * @return the number of comments
     */
    long countByIdeaId(Long ideaId);

    /**
     * Counts the comments of every idea in one query, so the ideas list doesn't need one query per idea.
     *
     * @return one {@code [idea id, number of comments]} pair per idea that has comments
     */
    @Query("select c.idea.id, count(c) from IdeaComment c group by c.idea.id")
    List<Object[]> countPerIdea();
}

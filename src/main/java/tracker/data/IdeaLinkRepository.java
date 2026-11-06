package tracker.data;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Stores and finds {@link IdeaLink}s. */
public interface IdeaLinkRepository extends JpaRepository<IdeaLink, Long> {

    /**
     * Lists every idea link, oldest first, so the ideas list can group them without a query per idea.
     *
     * @return all idea links
     */
    List<IdeaLink> findAllByOrderById();

    /**
     * Lists an idea's links in the order they were added.
     *
     * @param ideaId the idea's id
     * @return its links
     */
    List<IdeaLink> findByIdeaIdOrderById(Long ideaId);

    /**
     * Finds a link only if it belongs to the given idea.
     *
     * @param id the link's id
     * @param ideaId the idea's id
     * @return the link, or empty if there is no such link on that idea
     */
    Optional<IdeaLink> findByIdAndIdeaId(Long id, Long ideaId);
}

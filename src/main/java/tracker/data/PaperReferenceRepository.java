package tracker.data;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** Stores and finds {@link PaperReference}s. */
public interface PaperReferenceRepository extends JpaRepository<PaperReference, Long> {

    /**
     * Lists a paper's references in their original order.
     *
     * @param paperId the paper's id
     * @return the references; empty if none are known
     */
    List<PaperReference> findByPaperIdOrderByPosition(Long paperId);

    /**
     * Removes a paper's references, so a new lookup can replace them.
     *
     * @param paperId the paper's id
     */
    @Modifying(flushAutomatically = true)
    @Query("delete from PaperReference r where r.paper.id = :paperId")
    void deleteByPaperId(Long paperId);
}

package tracker.data;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaperShareRepository extends JpaRepository<PaperShare, Long> {

    @Query("select s from PaperShare s join fetch s.paper join fetch s.slackMessage "
            + "where s.paper.id in :paperIds order by s.id")
    List<PaperShare> findByPaperIds(@Param("paperIds") Collection<Long> paperIds);
}

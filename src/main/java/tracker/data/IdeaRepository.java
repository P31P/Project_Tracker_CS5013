package tracker.data;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdeaRepository extends JpaRepository<Idea, Long> {

    List<Idea> findAllByOrderByCreatedAtDescIdDesc();
}

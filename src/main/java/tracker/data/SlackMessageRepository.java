package tracker.data;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SlackMessageRepository extends JpaRepository<SlackMessage, Long> {

    Optional<SlackMessage> findByChannelIdAndMessageTs(String channelId, String messageTs);
}

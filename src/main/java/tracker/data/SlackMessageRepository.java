package tracker.data;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Stores and finds {@link SlackMessage}s. */
public interface SlackMessageRepository extends JpaRepository<SlackMessage, Long> {

    /**
     * Finds a message by its Slack identity.
     *
     * @param channelId the Slack channel id
     * @param messageTs the message timestamp
     * @return the message, or empty if it was never recorded
     */
    Optional<SlackMessage> findByChannelIdAndMessageTs(String channelId, String messageTs);
}

package tracker.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;

/** A Slack message that mentioned the bot, stored once per channel and timestamp. */
@Entity
@Table(name = "slack_message")
public class SlackMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "channel_id", nullable = false)
    private String channelId;

    @Column(name = "message_ts", nullable = false)
    private String messageTs;

    @Column(name = "slack_user_id", nullable = false)
    private String slackUserId;

    @Column(name = "slack_user_name")
    private String slackUserName;

    @Column(nullable = false)
    private String text;

    private String permalink;

    @CreationTimestamp
    @Column(name = "received_at", nullable = false, updatable = false)
    private OffsetDateTime receivedAt;

    /** Used by JPA. */
    protected SlackMessage() {
    }

    /**
     * Records a message.
     *
     * @param channelId the Slack channel id
     * @param messageTs the message timestamp, Slack's id for a message in a channel
     * @param slackUserId the poster's Slack user id
     * @param text the message text
     */
    public SlackMessage(String channelId, String messageTs, String slackUserId, String text) {
        this.channelId = channelId;
        this.messageTs = messageTs;
        this.slackUserId = slackUserId;
        this.text = text;
    }

    /**
     * Returns the database id, or {@code null} before the message is saved.
     *
     * @return the database id, or {@code null} before the message is saved
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns the Slack channel id.
     *
     * @return the Slack channel id
     */
    public String getChannelId() {
        return channelId;
    }

    /**
     * Returns the message timestamp.
     *
     * @return the message timestamp
     */
    public String getMessageTs() {
        return messageTs;
    }

    /**
     * Returns the poster's Slack user id.
     *
     * @return the poster's Slack user id
     */
    public String getSlackUserId() {
        return slackUserId;
    }

    /**
     * Returns the poster's name, or {@code null} if Slack didn't give it.
     *
     * @return the poster's name, or {@code null} if Slack didn't give it
     */
    public String getSlackUserName() {
        return slackUserName;
    }

    /**
     * Sets the poster's name.
     *
     * @param slackUserName the name; may be {@code null}
     */
    public void setSlackUserName(String slackUserName) {
        this.slackUserName = slackUserName;
    }

    /**
     * Returns the message text.
     *
     * @return the message text
     */
    public String getText() {
        return text;
    }

    /**
     * Returns a link to the message in Slack, or {@code null} if unknown.
     *
     * @return a link to the message in Slack, or {@code null} if unknown
     */
    public String getPermalink() {
        return permalink;
    }

    /**
     * Sets the link to the message in Slack.
     *
     * @param permalink the link; may be {@code null}
     */
    public void setPermalink(String permalink) {
        this.permalink = permalink;
    }

    /**
     * Returns when the app received the message.
     *
     * @return when the app received the message
     */
    public OffsetDateTime getReceivedAt() {
        return receivedAt;
    }
}

package tracker.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;

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

    protected SlackMessage() {
    }

    public SlackMessage(String channelId, String messageTs, String slackUserId, String text) {
        this.channelId = channelId;
        this.messageTs = messageTs;
        this.slackUserId = slackUserId;
        this.text = text;
    }

    public Long getId() {
        return id;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getMessageTs() {
        return messageTs;
    }

    public String getSlackUserId() {
        return slackUserId;
    }

    public String getSlackUserName() {
        return slackUserName;
    }

    public void setSlackUserName(String slackUserName) {
        this.slackUserName = slackUserName;
    }

    public String getText() {
        return text;
    }

    public String getPermalink() {
        return permalink;
    }

    public void setPermalink(String permalink) {
        this.permalink = permalink;
    }

    public OffsetDateTime getReceivedAt() {
        return receivedAt;
    }
}

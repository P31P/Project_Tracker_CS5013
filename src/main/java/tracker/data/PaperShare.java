package tracker.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** One time a paper was posted in Slack: which message, and the link exactly as posted. */
@Entity
@Table(name = "paper_share")
public class PaperShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paper_id")
    private Paper paper;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "slack_message_id")
    private SlackMessage slackMessage;

    @Column(name = "posted_url", nullable = false)
    private String postedUrl;

    /** Used by JPA. */
    protected PaperShare() {
    }

    /**
     * Records a share.
     *
     * @param paper the paper that was shared
     * @param slackMessage the message it was in
     * @param postedUrl the link as it appeared in the message
     */
    public PaperShare(Paper paper, SlackMessage slackMessage, String postedUrl) {
        this.paper = paper;
        this.slackMessage = slackMessage;
        this.postedUrl = postedUrl;
    }

    /**
     * Returns the database id, or {@code null} before the share is saved.
     *
     * @return the database id, or {@code null} before the share is saved
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns the paper that was shared.
     *
     * @return the paper that was shared
     */
    public Paper getPaper() {
        return paper;
    }

    /**
     * Returns the message the paper was in.
     *
     * @return the message the paper was in
     */
    public SlackMessage getSlackMessage() {
        return slackMessage;
    }

    /**
     * Returns the link as it appeared in the message.
     *
     * @return the link as it appeared in the message
     */
    public String getPostedUrl() {
        return postedUrl;
    }
}

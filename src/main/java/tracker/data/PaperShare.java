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

    protected PaperShare() {
    }

    public PaperShare(Paper paper, SlackMessage slackMessage, String postedUrl) {
        this.paper = paper;
        this.slackMessage = slackMessage;
        this.postedUrl = postedUrl;
    }

    public Long getId() {
        return id;
    }

    public Paper getPaper() {
        return paper;
    }

    public SlackMessage getSlackMessage() {
        return slackMessage;
    }

    public String getPostedUrl() {
        return postedUrl;
    }
}

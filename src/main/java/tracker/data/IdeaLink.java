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
import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;

/** A web link on an idea, such as a related article or a dataset. Only http(s) addresses are stored. */
@Entity
@Table(name = "idea_link")
public class IdeaLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idea_id")
    private Idea idea;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private String url;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** Used by JPA. */
    protected IdeaLink() {
    }

    /**
     * Creates a link.
     *
     * @param idea the idea it belongs to
     * @param label the text to show
     * @param url the address; must start with {@code http://} or {@code https://}
     */
    public IdeaLink(Idea idea, String label, String url) {
        this.idea = idea;
        this.label = label;
        this.url = url;
    }

    /**
     * Returns the database id, or {@code null} before the link is saved.
     *
     * @return the database id, or {@code null} before the link is saved
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns the idea the link belongs to.
     *
     * @return the idea
     */
    public Idea getIdea() {
        return idea;
    }

    /**
     * Returns the text to show.
     *
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns the address.
     *
     * @return the address
     */
    public String getUrl() {
        return url;
    }
}

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

/** A comment in the discussion under an {@link Idea}. */
@Entity
@Table(name = "idea_comment")
public class IdeaComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idea_id")
    private Idea idea;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private AppUser author;

    @Column(nullable = false)
    private String body;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** Used by JPA. */
    protected IdeaComment() {
    }

    /**
     * Creates a comment.
     *
     * @param idea the idea it is about
     * @param author who wrote it; may be {@code null}
     * @param body the text; must not be blank
     */
    public IdeaComment(Idea idea, AppUser author, String body) {
        this.idea = idea;
        this.author = author;
        this.body = body;
    }

    /**
     * Returns the database id, or {@code null} before the comment is saved.
     *
     * @return the database id, or {@code null} before the comment is saved
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns the idea the comment is about.
     *
     * @return the idea the comment is about
     */
    public Idea getIdea() {
        return idea;
    }

    /**
     * Returns who wrote the comment, or {@code null} if unknown.
     *
     * @return who wrote the comment, or {@code null} if unknown
     */
    public AppUser getAuthor() {
        return author;
    }

    /**
     * Returns the comment's text.
     *
     * @return the comment's text
     */
    public String getBody() {
        return body;
    }

    /**
     * Returns when the comment was written.
     *
     * @return when the comment was written
     */
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}

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

/**
 * A comment in a project's discussion. Unlike a timeline update, it is a conversation about the project rather
 * than news of its progress.
 */
@Entity
@Table(name = "project_comment")
public class ProjectComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private AppUser author;

    @Column(nullable = false)
    private String body;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** Used by JPA. */
    protected ProjectComment() {
    }

    /**
     * Creates a comment.
     *
     * @param project the project being discussed
     * @param author who wrote it; may be {@code null}
     * @param body the comment; must not be blank
     */
    public ProjectComment(Project project, AppUser author, String body) {
        this.project = project;
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
     * Returns who wrote the comment, or {@code null} if unknown.
     *
     * @return the author, or {@code null}
     */
    public AppUser getAuthor() {
        return author;
    }

    /**
     * Returns the comment's text.
     *
     * @return the text
     */
    public String getBody() {
        return body;
    }

    /**
     * Returns when the comment was written.
     *
     * @return when it was written
     */
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}

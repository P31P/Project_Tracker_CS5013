package tracker.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;

/** One entry on a project's timeline: a note someone wrote or a logged status change. */
@Entity
@Table(name = "project_update")
public class ProjectUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private AppUser author;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UpdateKind kind;

    @Column(nullable = false)
    private String body;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** Used by JPA. */
    protected ProjectUpdate() {
    }

    /**
     * Creates a timeline entry.
     *
     * @param project the project it belongs to
     * @param author who wrote it or changed the status; may be {@code null}
     * @param kind whether it is a note or a status change
     * @param body the text; must not be blank
     */
    public ProjectUpdate(Project project, AppUser author, UpdateKind kind, String body) {
        this.project = project;
        this.author = author;
        this.kind = kind;
        this.body = body;
    }

    /**
     * Returns the database id, or {@code null} before the entry is saved.
     *
     * @return the database id, or {@code null} before the entry is saved
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns the project the entry belongs to.
     *
     * @return the project the entry belongs to
     */
    public Project getProject() {
        return project;
    }

    /**
     * Returns who wrote the entry, or {@code null} if unknown.
     *
     * @return who wrote the entry, or {@code null} if unknown
     */
    public AppUser getAuthor() {
        return author;
    }

    /**
     * Returns whether the entry is a note or a status change.
     *
     * @return whether the entry is a note or a status change
     */
    public UpdateKind getKind() {
        return kind;
    }

    /**
     * Returns the entry's text.
     *
     * @return the entry's text
     */
    public String getBody() {
        return body;
    }

    /**
     * Returns when the entry was written.
     *
     * @return when the entry was written
     */
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}

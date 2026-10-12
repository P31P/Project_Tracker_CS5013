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

/** A link on a project, such as a repository, a document or a dataset. Only http(s) links are stored. */
@Entity
@Table(name = "project_link")
public class ProjectLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private String url;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** Used by JPA. */
    protected ProjectLink() {
    }

    /**
     * Creates a link.
     *
     * @param project the project it belongs to
     * @param label the text to show; must not be blank
     * @param url the address; must start with {@code http://} or {@code https://}
     */
    public ProjectLink(Project project, String label, String url) {
        this.project = project;
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
     * Returns the project the link belongs to.
     *
     * @return the project the link belongs to
     */
    public Project getProject() {
        return project;
    }

    /**
     * Returns the text shown for the link.
     *
     * @return the text shown for the link
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns the link's address.
     *
     * @return the link's address
     */
    public String getUrl() {
        return url;
    }
}

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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A lab project: a title, an optional description, a status, tags, an owner and members.
 *
 * <p>Only the title is required. Updates on its timeline are {@link ProjectUpdate}s and its links are
 * {@link ProjectLink}s.
 */
@Entity
@Table(name = "project")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatus status = ProjectStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private AppUser owner;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]", nullable = false)
    private List<String> tags = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "project_member",
            joinColumns = @JoinColumn(name = "project_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<AppUser> members = new LinkedHashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    /** Used by JPA. */
    protected Project() {
    }

    /**
     * Creates an active project with only a title.
     *
     * @param title the project's title; must not be blank
     */
    public Project(String title) {
        this.title = title;
    }

    /**
     * Returns the database id, or {@code null} before the project is saved.
     *
     * @return the database id, or {@code null} before the project is saved
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns the project's title.
     *
     * @return the project's title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Changes the title.
     *
     * @param title the new title; must not be blank
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Returns the description, or {@code null} if there is none.
     *
     * @return the description, or {@code null} if there is none
     */
    public String getDescription() {
        return description;
    }

    /**
     * Changes the description.
     *
     * @param description the new description; {@code null} removes it
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the project's status.
     *
     * @return the project's status
     */
    public ProjectStatus getStatus() {
        return status;
    }

    /**
     * Changes the status. This does not log the change; the caller adds a timeline entry.
     *
     * @param status the new status
     */
    public void setStatus(ProjectStatus status) {
        this.status = status;
    }

    /**
     * Returns the person who started the project, or {@code null} if unknown.
     *
     * @return the person who started the project, or {@code null} if unknown
     */
    public AppUser getOwner() {
        return owner;
    }

    /**
     * Sets who started the project.
     *
     * @param owner the owner; may be {@code null}
     */
    public void setOwner(AppUser owner) {
        this.owner = owner;
    }

    /**
     * Returns the project's tags, lowercased and without duplicates.
     *
     * @return the project's tags, lowercased and without duplicates
     */
    public List<String> getTags() {
        return tags;
    }

    /**
     * Replaces the tags with a copy of the given list.
     *
     * @param tags the new tags, already tidied by the caller
     */
    public void setTags(List<String> tags) {
        this.tags = new ArrayList<>(tags);
    }

    /**
     * Returns the people working on the project. Changes to the returned set are saved with the project.
     *
     * @return the project's members
     */
    public Set<AppUser> getMembers() {
        return members;
    }

    /**
     * Returns when the project was created.
     *
     * @return when the project was created
     */
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Returns when the project last changed.
     *
     * @return when the project last changed
     */
    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    /** Records that the project changed now, which moves it to the top of the list. */
    public void touch() {
        this.updatedAt = OffsetDateTime.now();
    }
}

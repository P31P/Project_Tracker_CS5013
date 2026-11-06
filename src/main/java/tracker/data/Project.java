package tracker.data;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A lab project: a title, an optional description, a status, a start date, a longer write-up, whether others can
 * join, tags, an owner and members.
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

    @Column(name = "started_on", nullable = false)
    private LocalDate startedOn = LocalDate.now();

    @Embedded
    private ResearchDetails details = new ResearchDetails();

    @Column(name = "open_for_collab", nullable = false)
    private boolean openForCollab;

    @Column(name = "collab_opened_at")
    private OffsetDateTime collabOpenedAt;

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

    /**
     * Returns the day the project started.
     *
     * @return the start date
     */
    public LocalDate getStartedOn() {
        return startedOn;
    }

    /**
     * Changes the start date.
     *
     * @param startedOn the new start date; must not be {@code null}
     */
    public void setStartedOn(LocalDate startedOn) {
        this.startedOn = startedOn;
    }

    /**
     * Returns the longer write-up: core question, motivation, approach, resources and challenges.
     *
     * @return the details; never {@code null}, though every part may be
     */
    public ResearchDetails getDetails() {
        // Hibernate loads an embedded value whose columns are all empty as null
        return details == null ? new ResearchDetails() : details;
    }

    /**
     * Replaces the longer write-up.
     *
     * @param details the new details, already tidied by the caller
     */
    public void setDetails(ResearchDetails details) {
        this.details = details;
    }

    /**
     * Says whether others can ask to join the project.
     *
     * @return {@code true} if it is open for collaboration, {@code false} if it is closed or a solo project
     */
    public boolean isOpenForCollab() {
        return openForCollab;
    }

    /**
     * Opens or closes the project for collaboration. Opening it records when, so Home can tell people about it.
     *
     * @param open {@code true} to open it, {@code false} to close it
     */
    public void setOpenForCollab(boolean open) {
        if (open && !openForCollab) {
            collabOpenedAt = OffsetDateTime.now();
        } else if (!open) {
            collabOpenedAt = null;
        }
        openForCollab = open;
    }

    /**
     * Returns when the project was last opened for collaboration, or {@code null} if it isn't open.
     *
     * @return when it was opened, or {@code null}
     */
    public OffsetDateTime getCollabOpenedAt() {
        return collabOpenedAt;
    }

    /** Records that the project changed now, which moves it to the top of the list. */
    public void touch() {
        this.updatedAt = OffsetDateTime.now();
    }
}

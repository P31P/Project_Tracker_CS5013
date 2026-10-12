package tracker.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** An idea someone noted down. It can collect votes and can become a {@link Project} once. */
@Entity
@Table(name = "idea")
public class Idea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private AppUser author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]", nullable = false)
    private List<String> tags = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "idea_vote",
            joinColumns = @JoinColumn(name = "idea_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<AppUser> voters = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** Used by JPA. */
    protected Idea() {
    }

    /**
     * Creates an idea with no tags or votes.
     *
     * @param title the idea; must not be blank
     * @param description more detail; may be {@code null}
     * @param author who wrote it; may be {@code null}
     */
    public Idea(String title, String description, AppUser author) {
        this.title = title;
        this.description = description;
        this.author = author;
    }

    /**
     * Returns the database id, or {@code null} before the idea is saved.
     *
     * @return the database id, or {@code null} before the idea is saved
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns the idea's title.
     *
     * @return the idea's title
     */
    public String getTitle() {
        return title;
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
     * Returns who wrote the idea, or {@code null} if unknown.
     *
     * @return who wrote the idea, or {@code null} if unknown
     */
    public AppUser getAuthor() {
        return author;
    }

    /**
     * Returns the project the idea became, or {@code null} if it is still open.
     *
     * @return the project the idea became, or {@code null} if it is still open
     */
    public Project getProject() {
        return project;
    }

    /**
     * Records that the idea became a project.
     *
     * @param project the project started from this idea
     */
    public void promotedTo(Project project) {
        this.project = project;
    }

    /**
     * Returns the idea's tags, lowercased and without duplicates.
     *
     * @return the idea's tags, lowercased and without duplicates
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
     * Returns the people who voted for the idea. Changes to the returned set are saved with the idea.
     *
     * @return the voters
     */
    public Set<AppUser> getVoters() {
        return voters;
    }

    /**
     * Returns when the idea was written.
     *
     * @return when the idea was written
     */
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}

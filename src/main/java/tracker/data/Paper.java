package tracker.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A paper shared in Slack, stored once per normalized URL. The worker fills in its details later. */
@Entity
@Table(name = "paper")
public class Paper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "normalized_url", nullable = false, unique = true)
    private String normalizedUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaperStatus status = PaperStatus.PENDING;

    private String title;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private List<String> authors;

    @Column(name = "published_on")
    private LocalDate publishedOn;

    private String doi;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]", nullable = false)
    private List<String> tags = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private PaperSource source;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at")
    private OffsetDateTime nextAttemptAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** Used by JPA. */
    protected Paper() {
    }

    /**
     * Creates a pending paper.
     *
     * @param normalizedUrl the paper's address as produced by {@code LinkExtractor}
     */
    public Paper(String normalizedUrl) {
        this.normalizedUrl = normalizedUrl;
    }

    /**
     * Returns the database id, or {@code null} before the paper is saved.
     *
     * @return the database id, or {@code null} before the paper is saved
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns the paper's normalized address.
     *
     * @return the paper's normalized address
     */
    public String getNormalizedUrl() {
        return normalizedUrl;
    }

    /**
     * Returns how far the lookup of the paper's details got.
     *
     * @return how far the lookup of the paper's details got
     */
    public PaperStatus getStatus() {
        return status;
    }

    /**
     * Changes the status.
     *
     * @param status the new status
     */
    public void setStatus(PaperStatus status) {
        this.status = status;
    }

    /**
     * Returns the title, or {@code null} until the paper is resolved.
     *
     * @return the title, or {@code null} until the paper is resolved
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns the authors in order, or {@code null} until the paper is resolved.
     *
     * @return the authors in order, or {@code null} until the paper is resolved
     */
    public List<String> getAuthors() {
        return authors;
    }

    /**
     * Returns the publication date, or {@code null} if unknown.
     *
     * @return the publication date, or {@code null} if unknown
     */
    public LocalDate getPublishedOn() {
        return publishedOn;
    }

    /**
     * Returns the DOI, or {@code null} if unknown.
     *
     * @return the DOI, or {@code null} if unknown
     */
    public String getDoi() {
        return doi;
    }

    /**
     * Returns the paper's tags, lowercased and without duplicates.
     *
     * @return the paper's tags, lowercased and without duplicates
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
     * Returns where the details came from, or {@code null} until the paper is resolved.
     *
     * @return where the details came from, or {@code null} until the paper is resolved
     */
    public PaperSource getSource() {
        return source;
    }

    /**
     * Returns how many lookups have failed so far.
     *
     * @return how many lookups have failed so far
     */
    public int getAttempts() {
        return attempts;
    }

    /**
     * Returns the earliest time of the next lookup, or {@code null} if it may run now.
     *
     * @return the earliest time of the next lookup, or {@code null} if it may run now
     */
    public OffsetDateTime getNextAttemptAt() {
        return nextAttemptAt;
    }

    /**
     * Returns when the paper was first shared.
     *
     * @return when the paper was first shared
     */
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Stores the details a resolver found and marks the paper resolved.
     *
     * @param title the title
     * @param authors the authors in order
     * @param publishedOn the publication date; may be {@code null}
     * @param doi the DOI; may be {@code null}
     * @param source where the details came from
     */
    public void markResolved(String title, List<String> authors, LocalDate publishedOn, String doi,
                             PaperSource source) {
        this.title = title;
        this.authors = authors;
        this.publishedOn = publishedOn;
        this.doi = doi;
        this.source = source;
        this.status = PaperStatus.RESOLVED;
    }

    /** Marks the paper as one whose details could not be found. */
    public void markUnresolved() {
        this.status = PaperStatus.UNRESOLVED;
    }

    /**
     * Counts a failed lookup and pushes the next one back. The wait grows with each failure.
     *
     * @param delayPerAttempt the wait after the first failure; the n-th failure waits n times as long
     */
    public void recordFailedAttempt(Duration delayPerAttempt) {
        this.attempts++;
        this.nextAttemptAt = OffsetDateTime.now().plus(delayPerAttempt.multipliedBy(attempts));
    }
}

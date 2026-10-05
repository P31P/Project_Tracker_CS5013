package tracker.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    @Enumerated(EnumType.STRING)
    private PaperSource source;

    @Column(nullable = false)
    private int attempts;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Paper() {
    }

    public Paper(String normalizedUrl) {
        this.normalizedUrl = normalizedUrl;
    }

    public Long getId() {
        return id;
    }

    public String getNormalizedUrl() {
        return normalizedUrl;
    }

    public PaperStatus getStatus() {
        return status;
    }

    public void setStatus(PaperStatus status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public LocalDate getPublishedOn() {
        return publishedOn;
    }

    public String getDoi() {
        return doi;
    }

    public PaperSource getSource() {
        return source;
    }

    public int getAttempts() {
        return attempts;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void markResolved(String title, List<String> authors, LocalDate publishedOn, String doi,
                             PaperSource source) {
        this.title = title;
        this.authors = authors;
        this.publishedOn = publishedOn;
        this.doi = doi;
        this.source = source;
        this.status = PaperStatus.RESOLVED;
    }

    public void markUnresolved() {
        this.status = PaperStatus.UNRESOLVED;
    }

    public void recordFailedAttempt() {
        this.attempts++;
    }
}

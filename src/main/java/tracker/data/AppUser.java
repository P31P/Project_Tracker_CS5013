package tracker.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A person who can be allowed to sign in, stored in {@code app_user}, with the profile they fill in. */
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    private String name;

    @Column(nullable = false)
    private boolean allowlisted;

    private String bio;

    private String background;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]", nullable = false)
    private List<String> interests = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** Used by JPA. */
    protected AppUser() {
    }

    /**
     * Creates a user who is not allowlisted yet.
     *
     * @param email the user's email address, stored lowercased
     * @param name the user's name; may be {@code null}
     */
    public AppUser(String email, String name) {
        this.email = email;
        this.name = name;
    }

    /**
     * Returns the database id, or {@code null} before the user is saved.
     *
     * @return the database id, or {@code null} before the user is saved
     */
    public Long getId() {
        return id;
    }

    /**
     * Returns the user's email address.
     *
     * @return the user's email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * Returns the user's name, or {@code null} if unknown.
     *
     * @return the user's name, or {@code null} if unknown
     */
    public String getName() {
        return name;
    }

    /**
     * Returns whether the user may sign in.
     *
     * @return whether the user may sign in
     */
    public boolean isAllowlisted() {
        return allowlisted;
    }

    /**
     * Sets whether the user may sign in.
     *
     * @param allowlisted {@code true} to let the user sign in
     */
    public void setAllowlisted(boolean allowlisted) {
        this.allowlisted = allowlisted;
    }

    /**
     * Returns when the user was added.
     *
     * @return when the user was added
     */
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Returns the person's introduction, in Markdown, or {@code null} if they haven't written one.
     *
     * @return the introduction, or {@code null}
     */
    public String getBio() {
        return bio;
    }

    /**
     * Changes the introduction.
     *
     * @param bio the new introduction; may be {@code null}
     */
    public void setBio(String bio) {
        this.bio = bio;
    }

    /**
     * Returns the person's study background, such as "PhD, systems biology", or {@code null}.
     *
     * @return the background, or {@code null}
     */
    public String getBackground() {
        return background;
    }

    /**
     * Changes the study background.
     *
     * @param background the new background; may be {@code null}
     */
    public void setBackground(String background) {
        this.background = background;
    }

    /**
     * Returns the person's fields of interest, as tags.
     *
     * @return the interests, lowercased and without duplicates
     */
    public List<String> getInterests() {
        return interests;
    }

    /**
     * Replaces the interests with a copy of the given list.
     *
     * @param interests the new interests, already tidied by the caller
     */
    public void setInterests(List<String> interests) {
        this.interests = new ArrayList<>(interests);
    }
}

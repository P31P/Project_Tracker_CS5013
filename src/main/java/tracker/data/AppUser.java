package tracker.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;

/** A person who can be allowed to sign in, stored in {@code app_user}. */
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
}

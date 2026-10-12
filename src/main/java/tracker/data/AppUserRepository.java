package tracker.data;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Stores and finds {@link AppUser}s. */
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    /**
     * Finds a user by email address. The match is exact, so pass the address lowercased.
     *
     * @param email the lowercased email address
     * @return the user, or empty if there is none
     */
    Optional<AppUser> findByEmail(String email);

    /**
     * Lists everyone who may sign in, by name and then email.
     *
     * @return the allowlisted users
     */
    List<AppUser> findByAllowlistedTrueOrderByNameAscEmailAsc();
}

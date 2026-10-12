package tracker.auth;

import java.util.Locale;
import org.springframework.stereotype.Service;
import tracker.data.AppUser;
import tracker.data.AppUserRepository;

/**
 * Decides who may sign in: only people with an {@code app_user} row marked as allowlisted.
 */
@Service
public class AllowlistService {

    private final AppUserRepository users;

    /**
     * Creates the service.
     *
     * @param users where the allowlisted users are stored
     */
    public AllowlistService(AppUserRepository users) {
        this.users = users;
    }

    /**
     * Checks whether an email address may sign in. The comparison ignores case.
     *
     * @param email the address Google reported for the user; may be {@code null}
     * @return {@code true} if a user with this email exists and is allowlisted
     */
    public boolean isAllowed(String email) {
        if (email == null) {
            return false;
        }
        return users.findByEmail(email.toLowerCase(Locale.ROOT))
                .map(AppUser::isAllowlisted)
                .orElse(false);
    }
}

package tracker.auth;

import java.util.Locale;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import tracker.data.AppUser;
import tracker.data.AppUserRepository;

/**
 * Finds the {@code app_user} row of whoever is signed in.
 */
@Component
public class CurrentUser {

    private final AppUserRepository users;

    /**
     * Creates the helper.
     *
     * @param users where users are stored
     */
    public CurrentUser(AppUserRepository users) {
        this.users = users;
    }

    /**
     * Looks up the stored user for a signed-in Google user. The email comparison ignores case.
     *
     * @param user the signed-in user; may be {@code null}
     * @return the matching user, or {@code null} if there is no signed-in user or no row for their email
     */
    public AppUser of(OidcUser user) {
        if (user == null || user.getEmail() == null) {
            return null;
        }
        return users.findByEmail(user.getEmail().toLowerCase(Locale.ROOT)).orElse(null);
    }

    /**
     * Returns the name to show for a user: their name if known, otherwise their email.
     *
     * @param user the user; may be {@code null}
     * @return the display name, or {@code null} if {@code user} is {@code null}
     */
    public static String nameOf(AppUser user) {
        if (user == null) {
            return null;
        }
        return user.getName() != null ? user.getName() : user.getEmail();
    }
}

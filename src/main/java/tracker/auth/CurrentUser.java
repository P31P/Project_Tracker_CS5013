package tracker.auth;

import java.util.Locale;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import tracker.data.AppUser;
import tracker.data.AppUserRepository;

/** Finds the app_user row of whoever is signed in. */
@Component
public class CurrentUser {

    private final AppUserRepository users;

    public CurrentUser(AppUserRepository users) {
        this.users = users;
    }

    public AppUser of(OidcUser user) {
        if (user == null || user.getEmail() == null) {
            return null;
        }
        return users.findByEmail(user.getEmail().toLowerCase(Locale.ROOT)).orElse(null);
    }

    public static String nameOf(AppUser user) {
        if (user == null) {
            return null;
        }
        return user.getName() != null ? user.getName() : user.getEmail();
    }
}

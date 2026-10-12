package tracker.auth;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tells the page who is signed in.
 */
@RestController
public class MeController {

    /**
     * The signed-in user, as returned by {@code GET /api/me}.
     *
     * @param email the user's email address
     * @param name the user's full name from Google; may be {@code null}
     */
    public record Me(String email, String name) {
    }

    /**
     * Returns the signed-in user. Handles {@code GET /api/me}.
     *
     * @param user the signed-in user
     * @return the user's email and name
     */
    @GetMapping("/api/me")
    public Me me(@AuthenticationPrincipal OidcUser user) {
        return new Me(user.getEmail(), user.getFullName());
    }
}

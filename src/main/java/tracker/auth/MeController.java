package tracker.auth;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tracker.data.AppUser;
import tracker.data.Role;

/**
 * Tells the page who is signed in.
 */
@RestController
public class MeController {

    private final CurrentUser currentUser;

    /**
     * Creates the controller.
     *
     * @param currentUser finds the signed-in user's stored row
     */
    public MeController(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    /**
     * The signed-in user.
     *
     * @param email their email address
     * @param name their name from Google
     * @param role what they may do, so the page knows whether to offer Manage access
     */
    public record Me(String email, String name, Role role) {
    }

    /**
     * Returns the signed-in user. Handles {@code GET /api/me}.
     *
     * @param user the signed-in user
     * @return their email, name and role
     */
    @GetMapping("/api/me")
    public Me me(@AuthenticationPrincipal OidcUser user) {
        AppUser stored = currentUser.of(user);
        return new Me(user.getEmail(), user.getFullName(), stored == null ? Role.MEMBER : stored.getRole());
    }
}

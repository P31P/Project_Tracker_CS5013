package tracker.profile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tracker.auth.CurrentUser;

/** The signed-in person's own profile, under {@code /api/profile}. */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profiles;
    private final CurrentUser currentUser;

    /**
     * Creates the controller.
     *
     * @param profiles reads and saves profiles
     * @param currentUser finds the signed-in user
     */
    public ProfileController(ProfileService profiles, CurrentUser currentUser) {
        this.profiles = profiles;
        this.currentUser = currentUser;
    }

    /**
     * Returns the signed-in person's profile. Handles {@code GET /api/profile}.
     *
     * @param user the signed-in user
     * @return their profile
     */
    @GetMapping
    public ProfileView mine(@AuthenticationPrincipal OidcUser user) {
        return profiles.mine(currentUser.of(user));
    }

    /**
     * Replaces the signed-in person's introduction, background and interests. Handles {@code PATCH /api/profile}.
     * Name and email come from Google and can't be changed here.
     *
     * @param edit the new profile
     * @param user the signed-in user
     * @return the saved profile
     */
    @PatchMapping
    public ProfileView update(@RequestBody ProfileEdit edit, @AuthenticationPrincipal OidcUser user) {
        return profiles.update(currentUser.of(user), edit);
    }
}

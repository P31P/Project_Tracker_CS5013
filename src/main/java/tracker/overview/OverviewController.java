package tracker.overview;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tracker.auth.CurrentUser;
import tracker.overview.OverviewService.Overview;

/** Serves the Home tab's data. */
@RestController
public class OverviewController {

    private final OverviewService overview;
    private final CurrentUser currentUser;

    /**
     * Creates the controller.
     *
     * @param overview builds the overview
     * @param currentUser finds the signed-in user
     */
    public OverviewController(OverviewService overview, CurrentUser currentUser) {
        this.overview = overview;
        this.currentUser = currentUser;
    }

    /**
     * Returns the Home tab's numbers, the user's projects and the latest activity. Handles {@code GET /api/overview}.
     *
     * @param user the signed-in user
     * @return the overview
     */
    @GetMapping("/api/overview")
    public Overview overview(@AuthenticationPrincipal OidcUser user) {
        return overview.overview(currentUser.of(user));
    }
}

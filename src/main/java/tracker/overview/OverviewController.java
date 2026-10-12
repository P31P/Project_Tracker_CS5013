package tracker.overview;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tracker.auth.CurrentUser;
import tracker.overview.OverviewService.Overview;

@RestController
public class OverviewController {

    private final OverviewService overview;
    private final CurrentUser currentUser;

    public OverviewController(OverviewService overview, CurrentUser currentUser) {
        this.overview = overview;
        this.currentUser = currentUser;
    }

    @GetMapping("/api/overview")
    public Overview overview(@AuthenticationPrincipal OidcUser user) {
        return overview.overview(currentUser.of(user));
    }
}

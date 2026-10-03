package tracker.auth;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

    public record Me(String email, String name) {
    }

    @GetMapping("/api/me")
    public Me me(@AuthenticationPrincipal OidcUser user) {
        return new Me(user.getEmail(), user.getFullName());
    }
}

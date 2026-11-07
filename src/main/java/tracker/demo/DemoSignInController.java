package tracker.demo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Signs a grader in as the demo user, a plain member added by {@code docker/demo-user.sql}. Only in the
 * {@code demo} profile.
 */
@RestController
@Profile("demo")
public class DemoSignInController {

    /** The demo user's email; their row in {@code app_user} must be allowlisted. */
    static final String DEMO_EMAIL = "demo@example.com";

    private final SecurityContextRepository contexts = new HttpSessionSecurityContextRepository();

    /**
     * Answers {@code 204} so the sign-in page knows to offer the demo sign-in. Handles {@code GET /demo/available}.
     */
    @GetMapping("/demo/available")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void available() {
    }

    /**
     * Signs in as the demo user and opens the app. Handles {@code GET /demo/sign-in}. The user looks the same to
     * the rest of the app as someone signed in with Google, so no other code knows about demo mode.
     *
     * @param request the browser's request
     * @param response redirects to the app
     * @throws IOException if the redirect can't be sent
     */
    @GetMapping("/demo/sign-in")
    public void signIn(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Instant now = Instant.now();
        OidcIdToken token = new OidcIdToken("demo", now, now.plus(Duration.ofHours(8)),
                Map.of("sub", "demo", "email", DEMO_EMAIL, "name", "Demo User"));
        OidcUser user = new DefaultOidcUser(List.of(new OidcUserAuthority(token)), token);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new OAuth2AuthenticationToken(user, user.getAuthorities(), "google"));
        // a new session id at sign-in, as Google sign-in does
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        contexts.saveContext(context, request, response);
        response.sendRedirect("/");
    }
}

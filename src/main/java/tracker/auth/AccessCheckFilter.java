package tracker.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Locale;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.filter.OncePerRequestFilter;
import tracker.data.AppUserRepository;

/**
 * Signs out anyone whose access was removed while they were signed in. Google sign-in only checks the allowlist
 * once, so without this a removed person could keep using the tracker until their session ran out.
 *
 * <p>Made by {@link SecurityConfig} rather than as a bean, so it runs once, inside the security filters.
 */
public class AccessCheckFilter extends OncePerRequestFilter {

    private final AppUserRepository users;

    /**
     * Creates the filter.
     *
     * @param users where users are stored
     */
    public AccessCheckFilter(AppUserRepository users) {
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication signedIn = SecurityContextHolder.getContext().getAuthentication();
        if (signedIn != null && signedIn.getPrincipal() instanceof OidcUser user && user.getEmail() != null
                && removed(user.getEmail())) {
            SecurityContextHolder.clearContext();
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            if (request.getRequestURI().startsWith("/api/")) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Your access to the tracker was removed\"}");
            } else {
                response.sendRedirect("/login.html?error");
            }
            return;
        }
        chain.doFilter(request, response);
    }

    // only a stored row marked not allowlisted counts; anyone else got through Google sign-in's own check
    private boolean removed(String email) {
        return users.findByEmail(email.toLowerCase(Locale.ROOT)).map(user -> !user.isAllowlisted()).orElse(false);
    }
}

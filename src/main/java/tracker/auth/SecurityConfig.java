package tracker.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;

/**
 * Web security: Google sign-in limited to the allowlist, CSRF protection for the API, and sign-out.
 *
 * <p>Everything except the sign-in page, its stylesheets and fonts requires sign-in. Signed-out page requests are
 * redirected to {@code /login.html}; signed-out API requests get {@code 401}.
 */
@Configuration
public class SecurityConfig {

    private static final String LOGIN_PAGE = "/login.html";

    /**
     * Builds the security rules for every request.
     *
     * @param http Spring Security's builder
     * @return the filter chain that applies the rules
     * @throws Exception if Spring Security cannot build the chain
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(LOGIN_PAGE, "/css/**", "/fonts/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(login -> login.loginPage(LOGIN_PAGE))
                .logout(logout -> logout.logoutSuccessUrl(LOGIN_PAGE + "?logout"))
                // the CSRF token travels in an XSRF-TOKEN cookie that api.js sends back as a header
                .csrf(csrf -> csrf.spa())
                // the cookie is only written once something reads the token, so read it on every request
                .addFilterAfter((request, response, chain) -> {
                    CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
                    if (token != null) {
                        token.getToken();
                    }
                    chain.doFilter(request, response);
                }, CsrfFilter.class)
                // pages redirect to the sign-in page, but the API answers 401 so the page's JavaScript can react
                .exceptionHandling(errors -> errors
                        .defaultAuthenticationEntryPointFor(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                PathPatternRequestMatcher.withDefaults().matcher("/api/**"))
                        .defaultAuthenticationEntryPointFor(new LoginUrlAuthenticationEntryPoint(LOGIN_PAGE),
                                AnyRequestMatcher.INSTANCE));
        return http.build();
    }

    /**
     * Loads the signed-in user from Google and refuses anyone not on the allowlist. Runs right after Google
     * confirms who the user is.
     *
     * @param allowlist decides who may sign in
     * @return the user service Spring Security calls during sign-in
     */
    @Bean
    OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService(AllowlistService allowlist) {
        OidcUserService google = new OidcUserService();
        return request -> requireAllowlisted(google.loadUser(request), allowlist);
    }

    /**
     * Lets an allowlisted user through. Kept separate from {@link #oidcUserService} so tests can call it
     * without contacting Google.
     *
     * @param user the user Google signed in
     * @param allowlist decides who may sign in
     * @return the same user, if allowlisted
     * @throws OAuth2AuthenticationException with error code {@code not_allowlisted} if the user is not allowed
     */
    static OidcUser requireAllowlisted(OidcUser user, AllowlistService allowlist) {
        if (!allowlist.isAllowed(user.getEmail())) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("not_allowlisted"), user.getEmail() + " is not on the allowlist");
        }
        return user;
    }
}

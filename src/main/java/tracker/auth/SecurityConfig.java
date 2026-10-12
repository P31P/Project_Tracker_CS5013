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

@Configuration
public class SecurityConfig {

    private static final String LOGIN_PAGE = "/login.html";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(LOGIN_PAGE, "/css/**").permitAll()
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

    // Runs right after Google confirms who the user is
    @Bean
    OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService(AllowlistService allowlist) {
        OidcUserService google = new OidcUserService();
        return request -> requireAllowlisted(google.loadUser(request), allowlist);
    }

    // Separate from the bean above so tests can call it without contacting Google
    static OidcUser requireAllowlisted(OidcUser user, AllowlistService allowlist) {
        if (!allowlist.isAllowed(user.getEmail())) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("not_allowlisted"), user.getEmail() + " is not on the allowlist");
        }
        return user;
    }
}

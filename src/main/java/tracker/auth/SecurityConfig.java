package tracker.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(requests -> requests.anyRequest().authenticated())
                .oauth2Login(Customizer.withDefaults());
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

package tracker.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tracker.data.AppUser;
import tracker.data.AppUserRepository;
import tracker.support.PostgresTestBase;

@AutoConfigureMockMvc
@Transactional
class SecurityConfigTest extends PostgresTestBase {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AllowlistService allowlist;

    @Autowired
    private AppUserRepository users;

    @Test
    void signedOutVisitorIsSentToTheSignInPage() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login.html"));
    }

    @Test
    void signInPageAndItsStylesAreOpenToEveryone() throws Exception {
        mvc.perform(get("/login.html")).andExpect(status().isOk());
        mvc.perform(get("/css/app.css")).andExpect(status().isOk());
        mvc.perform(get("/fonts/geist.woff2")).andExpect(status().isOk());
    }

    @Test
    void scriptsStillNeedSignIn() throws Exception {
        mvc.perform(get("/js/app.js")).andExpect(status().is3xxRedirection());
    }

    // a refused sign-in lands here; if it redirected to Google too, the user would loop forever
    @Test
    void signInErrorPageIsShownWithoutSigningIn() throws Exception {
        mvc.perform(get("/login.html").param("error", ""))
                .andExpect(status().isOk());
    }

    @Test
    void signedInUserGetsThePage() throws Exception {
        mvc.perform(get("/").with(oidcLogin()))
                .andExpect(status().isOk());
    }

    @Test
    void signOutReturnsToTheSignInPage() throws Exception {
        mvc.perform(post("/logout").with(oidcLogin()).with(csrf()))
                .andExpect(redirectedUrl("/login.html?logout"));
    }

    @Test
    void signOutWithoutTheCsrfTokenIsRefused() throws Exception {
        mvc.perform(post("/logout").with(oidcLogin()))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowlistedGoogleUserIsLetIn() {
        AppUser member = new AppUser("member@example.com", "Member");
        member.setAllowlisted(true);
        users.saveAndFlush(member);
        OidcUser googleUser = googleUser("member@example.com");

        assertThat(SecurityConfig.requireAllowlisted(googleUser, allowlist)).isSameAs(googleUser);
    }

    @Test
    void googleUserNotOnAllowlistIsRefused() {
        assertThatThrownBy(() -> SecurityConfig.requireAllowlisted(googleUser("stranger@example.com"), allowlist))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    // A stand-in for the user Google would return after sign-in
    private static OidcUser googleUser(String email) {
        OidcIdToken token = OidcIdToken.withTokenValue("test-token")
                .claim("sub", email)
                .claim("email", email)
                .build();
        return new DefaultOidcUser(AuthorityUtils.createAuthorityList("OIDC_USER"), token);
    }
}

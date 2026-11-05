package tracker.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tracker.support.PostgresTestBase;

@AutoConfigureMockMvc
@Transactional
class AccessApiTest extends PostgresTestBase {

    // a Lab Manager since V18
    private static final RequestPostProcessor MANAGER = as("cs24b047@smail.iitm.ac.in");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private AllowlistService allowlist;

    @Test
    void aMemberCantManageAccess() throws Exception {
        user("member@example.com", "MEMBER", true);

        mvc.perform(get("/api/access").with(as("member@example.com")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Only a Lab Manager can manage access"));
    }

    @Test
    void aLabManagerAddsSomeoneWhoCanThenSignIn() throws Exception {
        send(post("/api/access"), MANAGER, "{\"email\": \"  New.Person@Example.com \", \"name\": \"New Person\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new.person@example.com"))
                .andExpect(jsonPath("$.role").value("MEMBER"))
                .andExpect(jsonPath("$.allowlisted").value(true));

        assertThat(allowlist.isAllowed("new.person@example.com")).isTrue();
        mvc.perform(get("/api/access").with(MANAGER))
                .andExpect(jsonPath("$[?(@.email == 'new.person@example.com')].name").value("New Person"));
    }

    @Test
    void someoneRemovedIsSignedOutOnTheirNextRequestAndCanBeRestored() throws Exception {
        long id = user("leaving@example.com", "MEMBER", true);
        mvc.perform(get("/api/projects").with(as("leaving@example.com"))).andExpect(status().isOk());

        send(patch("/api/access/" + id), MANAGER, "{\"allowlisted\": false}")
                .andExpect(jsonPath("$.allowlisted").value(false));

        mvc.perform(get("/api/projects").with(as("leaving@example.com")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Your access to the tracker was removed"));
        mvc.perform(get("/").with(as("leaving@example.com")))
                .andExpect(redirectedUrl("/login.html?error"));

        send(patch("/api/access/" + id), MANAGER, "{\"allowlisted\": true}");
        mvc.perform(get("/api/projects").with(as("leaving@example.com"))).andExpect(status().isOk());
    }

    @Test
    void aLabManagerChangesRolesButCantMakeAnAdmin() throws Exception {
        long id = user("helper@example.com", "MEMBER", true);

        send(patch("/api/access/" + id), MANAGER, "{\"role\": \"LAB_MANAGER\"}")
                .andExpect(jsonPath("$.role").value("LAB_MANAGER"));
        send(patch("/api/access/" + id), MANAGER, "{\"role\": \"ADMIN\"}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Only an admin can make or change an admin"));
        send(post("/api/access"), MANAGER, "{\"email\": \"boss@example.com\", \"role\": \"ADMIN\"}")
                .andExpect(status().isForbidden());
    }

    @Test
    void onlyAnAdminCanRemoveAnAdmin() throws Exception {
        long head = user("head@example.com", "ADMIN", true);
        user("second@example.com", "ADMIN", true);

        send(patch("/api/access/" + head), MANAGER, "{\"allowlisted\": false}")
                .andExpect(status().isForbidden());
        send(patch("/api/access/" + head), as("second@example.com"), "{\"role\": \"MEMBER\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MEMBER"));
    }

    @Test
    void nobodyChangesTheirOwnAccess() throws Exception {
        long me = jdbc.sql("SELECT id FROM app_user WHERE email = 'cs24b047@smail.iitm.ac.in'").query(Long.class).single();

        send(patch("/api/access/" + me), MANAGER, "{\"allowlisted\": false}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("You can't change your own access"));
    }

    @Test
    void aBadEmailIsRefused() throws Exception {
        send(post("/api/access"), MANAGER, "{\"email\": \"not an email\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Give the Google account's email address"));
    }

    @Test
    void theSignedInUsersRoleIsInMe() throws Exception {
        mvc.perform(get("/api/me").with(MANAGER)).andExpect(jsonPath("$.role").value("LAB_MANAGER"));
        user("member@example.com", "MEMBER", true);
        mvc.perform(get("/api/me").with(as("member@example.com"))).andExpect(jsonPath("$.role").value("MEMBER"));
    }

    @Test
    void changingAccessWithoutTheCsrfTokenIsRefused() throws Exception {
        mvc.perform(post("/api/access").with(MANAGER).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"x@example.com\"}"))
                .andExpect(status().isForbidden());
    }

    private static RequestPostProcessor as(String email) {
        return oidcLogin().idToken(token -> token.claim("email", email));
    }

    private long user(String email, String role, boolean allowlisted) {
        return jdbc.sql("INSERT INTO app_user (email, role, allowlisted) VALUES (:email, :role, :allowlisted) RETURNING id")
                .param("email", email).param("role", role).param("allowlisted", allowlisted)
                .query(Long.class).single();
    }

    private ResultActions send(MockHttpServletRequestBuilder request, RequestPostProcessor who, String json)
            throws Exception {
        return mvc.perform(request.with(who).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json));
    }
}

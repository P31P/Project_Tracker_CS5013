package tracker.profile;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tracker.support.PostgresTestBase;

@AutoConfigureMockMvc
@Transactional
class ProfileApiTest extends PostgresTestBase {

    // allowlisted by V3
    static final RequestPostProcessor PRIYAL = oidcLogin().idToken(token -> token
            .claim("email", "cs24b047@smail.iitm.ac.in").claim("name", "Priyal Patel"));

    @Autowired
    private MockMvc mvc;

    @Test
    void profileIsSavedAndReadBack() throws Exception {
        save("{\"bio\": \"  I work on **metabolic models**. \", \"background\": \"BTech, DSAI\","
                + " \"interests\": [\" #Genomics\", \"genomics\", \"Machine Learning\"]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value("I work on **metabolic models**."))
                .andExpect(jsonPath("$.interests.length()").value(2))
                .andExpect(jsonPath("$.interests[1]").value("machine learning"));

        mvc.perform(get("/api/profile").with(PRIYAL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Priyal Patel"))
                .andExpect(jsonPath("$.email").value("cs24b047@smail.iitm.ac.in"))
                .andExpect(jsonPath("$.background").value("BTech, DSAI"))
                .andExpect(jsonPath("$.interests[0]").value("genomics"));
    }

    @Test
    void theProfileSaysWhoManagesAccess() throws Exception {
        mvc.perform(get("/api/profile").with(PRIYAL)).andExpect(jsonPath("$.role").value("LAB_MANAGER"));
        // Home shows the lab-wide numbers to the admin
        mvc.perform(get("/api/profile")
                        .with(oidcLogin().idToken(token -> token.claim("email", "da24b026@smail.iitm.ac.in"))))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void savingReplacesEveryFieldAndBlanksBecomeEmpty() throws Exception {
        save("{\"bio\": \"Hello\", \"background\": \"PhD\", \"interests\": [\"genomics\"]}");

        save("{\"bio\": \"  \"}")
                .andExpect(jsonPath("$.bio").isEmpty())
                .andExpect(jsonPath("$.background").isEmpty())
                .andExpect(jsonPath("$.interests").isEmpty());
    }

    @Test
    void tooLongAnIntroductionIsRefused() throws Exception {
        save("{\"bio\": \"" + "a".repeat(ProfileService.MAX_BIO + 1) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("The introduction can be at most 2000 characters"));
    }

    @Test
    void tooManyInterestsAreRefused() throws Exception {
        StringBuilder many = new StringBuilder("[");
        for (int i = 0; i <= ProfileService.MAX_INTERESTS; i++) {
            many.append(i == 0 ? "" : ",").append("\"topic ").append(i).append('"');
        }
        save("{\"interests\": " + many + "]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Pick at most 20 interests"));
    }

    @Test
    void someoneWithoutAStoredAccountHasNoProfile() throws Exception {
        mvc.perform(get("/api/profile").with(oidcLogin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No profile for this account"));
    }

    @Test
    void savingWithoutTheCsrfTokenIsRefused() throws Exception {
        mvc.perform(patch("/api/profile").with(PRIYAL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void signedOutCallsGet401() throws Exception {
        mvc.perform(get("/api/profile")).andExpect(status().isUnauthorized());
    }

    private ResultActions save(String json) throws Exception {
        return mvc.perform(patch("/api/profile").with(PRIYAL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }
}

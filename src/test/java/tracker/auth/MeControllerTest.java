package tracker.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import tracker.support.PostgresTestBase;

@AutoConfigureMockMvc
class MeControllerTest extends PostgresTestBase {

    @Autowired
    private MockMvc mvc;

    @Test
    void returnsSignedInUser() throws Exception {
        mvc.perform(get("/api/me").with(oidcLogin().idToken(token -> token
                        .claim("email", "da24b026@smail.iitm.ac.in")
                        .claim("name", "Sujal Jayant Kumar"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("da24b026@smail.iitm.ac.in"))
                .andExpect(jsonPath("$.name").value("Sujal Jayant Kumar"));
    }

    @Test
    void signedOutVisitorIsSentToGoogle() throws Exception {
        mvc.perform(get("/api/me"))
                .andExpect(status().is3xxRedirection());
    }
}

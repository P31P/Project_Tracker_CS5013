package tracker.demo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import tracker.support.PostgresTestBase;

// without the demo profile, as on the lab's server, the demo paths are just more pages that need Google sign-in
@AutoConfigureMockMvc
class DemoModeOffTest extends PostgresTestBase {

    @Autowired
    private MockMvc mvc;

    @Test
    void thereIsNoDemoSignIn() throws Exception {
        mvc.perform(get("/demo/available")).andExpect(redirectedUrl("/login.html"));
        mvc.perform(get("/demo/sign-in")).andExpect(redirectedUrl("/login.html"));
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }
}

package tracker.autofill;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tracker.support.PostgresTestBase;

// tests run without a model (llm.enabled is false by default), which is also how every form must keep working
@AutoConfigureMockMvc
class AutoFillApiTest extends PostgresTestBase {

    @Autowired
    private MockMvc mvc;

    @Test
    void statusSaysAutoFillIsOff() throws Exception {
        mvc.perform(get("/api/autofill").with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void suggestingWhileOffExplainsWhy() throws Exception {
        mvc.perform(post("/api/autofill").with(oidcLogin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"note\": \"flux sampling\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Auto-fill is turned off"));
    }

    @Test
    void suggestingWithoutTheCsrfTokenIsRefused() throws Exception {
        mvc.perform(post("/api/autofill").with(oidcLogin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"note\": \"flux sampling\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void signedOutCallsGet401() throws Exception {
        mvc.perform(get("/api/autofill")).andExpect(status().isUnauthorized());
    }
}

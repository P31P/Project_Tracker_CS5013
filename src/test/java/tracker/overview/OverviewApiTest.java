package tracker.overview;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tracker.support.PostgresTestBase;

@AutoConfigureMockMvc
@Transactional
class OverviewApiTest extends PostgresTestBase {

    private static final RequestPostProcessor SUJAL = oidcLogin().idToken(token -> token
            .claim("email", "da24b026@smail.iitm.ac.in")
            .claim("name", "Sujal Jayant Kumar"));

    @Autowired
    private MockMvc mvc;

    @Test
    void showsMyProjectsAndTheLatestActivity() throws Exception {
        String project = mvc.perform(post("/api/projects").with(SUJAL).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\": \"Overview project\"}"))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(project, "$.id")).longValue();
        mvc.perform(post("/api/projects/" + id + "/updates").with(SUJAL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"body\": \"First results are in\"}"));

        mvc.perform(get("/api/overview").with(SUJAL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counts.active").value(Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.mine[0].title").value("Overview project"))
                .andExpect(jsonPath("$.activity[0].kind").value("NOTE"))
                .andExpect(jsonPath("$.activity[0].target").value("Overview project"))
                .andExpect(jsonPath("$.activity[0].excerpt").value("First results are in"));
    }
}

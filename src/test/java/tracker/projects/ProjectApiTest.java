package tracker.projects;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tracker.support.PostgresTestBase;

@AutoConfigureMockMvc
@Transactional
class ProjectApiTest extends PostgresTestBase {

    // allowlisted by V3, so the API can find the app_user row
    static final RequestPostProcessor SUJAL = oidcLogin().idToken(token -> token
            .claim("email", "da24b026@smail.iitm.ac.in")
            .claim("name", "Sujal Jayant Kumar"));

    @Autowired
    private MockMvc mvc;

    @Test
    void projectStartsWithOnlyATitle() throws Exception {
        send(post("/api/projects"), "{\"title\": \"Protein design\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Protein design"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.ownerName").value("Sujal Jayant Kumar"))
                .andExpect(jsonPath("$.timeline").isEmpty());
    }

    @Test
    void blankTitleIsRefusedWithAMessage() throws Exception {
        send(post("/api/projects"), "{\"title\": \"   \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("A project needs a title"));
    }

    @Test
    void creatingWithoutTheCsrfTokenIsRefused() throws Exception {
        mvc.perform(post("/api/projects").with(SUJAL)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\": \"No token\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void signedOutCallsGet401() throws Exception {
        mvc.perform(get("/api/projects")).andExpect(status().isUnauthorized());
    }

    @Test
    void statusChangeIsRecordedOnTheTimeline() throws Exception {
        long id = create("Genome-scale model");

        send(patch("/api/projects/" + id), "{\"status\": \"DONE\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.timeline[0].kind").value("STATUS"))
                .andExpect(jsonPath("$.timeline[0].body").value("Status changed from Active to Done"));
    }

    @Test
    void updatesAppearNewestFirst() throws Exception {
        long id = create("Metabolic network");
        send(post("/api/projects/" + id + "/updates"), "{\"body\": \"Collected the data\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorName").value("Sujal Jayant Kumar"));
        send(post("/api/projects/" + id + "/updates"), "{\"body\": \"First model runs\"}");

        mvc.perform(get("/api/projects/" + id).with(SUJAL))
                .andExpect(jsonPath("$.timeline[0].body").value("First model runs"))
                .andExpect(jsonPath("$.timeline[1].body").value("Collected the data"));
    }

    @Test
    void emptyUpdateIsRefused() throws Exception {
        long id = create("Empty update");

        send(post("/api/projects/" + id + "/updates"), "{\"body\": \"\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("An update can't be empty"));
    }

    @Test
    void listShowsTheMostRecentlyActiveProjectFirst() throws Exception {
        long older = create("Older project");
        create("Newer project");
        send(post("/api/projects/" + older + "/updates"), "{\"body\": \"Back on it\"}");

        mvc.perform(get("/api/projects").with(SUJAL))
                .andExpect(jsonPath("$[0].title").value("Older project"))
                .andExpect(jsonPath("$[0].updateCount").value(1));
    }

    @Test
    void unknownProjectGives404() throws Exception {
        mvc.perform(get("/api/projects/999999").with(SUJAL))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No project with id 999999"));
    }

    private long create(String title) throws Exception {
        String body = send(post("/api/projects"), "{\"title\": \"" + title + "\"}")
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private org.springframework.test.web.servlet.ResultActions send(MockHttpServletRequestBuilder request, String json)
            throws Exception {
        return mvc.perform(request.with(SUJAL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json));
    }
}

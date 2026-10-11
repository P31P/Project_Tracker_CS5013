package tracker.projects;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static tracker.projects.ProjectApiTest.SUJAL;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tracker.support.PostgresTestBase;

@AutoConfigureMockMvc
@Transactional
class IdeaApiTest extends PostgresTestBase {

    @Autowired
    private MockMvc mvc;

    @Test
    void ideaIsSavedWithItsAuthor() throws Exception {
        postIdea("{\"title\": \"Use Slack threads as project updates\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorName").value("Sujal Jayant Kumar"))
                .andExpect(jsonPath("$.projectId").isEmpty());

        mvc.perform(get("/api/ideas").with(SUJAL))
                .andExpect(jsonPath("$[0].title").value("Use Slack threads as project updates"));
    }

    @Test
    void blankTitleIsRefusedWithAMessage() throws Exception {
        postIdea("{\"title\": \"\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("An idea needs a title"));
    }

    @Test
    void promotingCreatesAPlanningProjectAndLinksIt() throws Exception {
        long ideaId = idOf(postIdea("{\"title\": \"Flux sampling benchmark\", \"description\": \"Compare samplers\"}"));

        String project = mvc.perform(post("/api/ideas/" + ideaId + "/promote").with(SUJAL).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Flux sampling benchmark"))
                .andExpect(jsonPath("$.description").value("Compare samplers"))
                .andExpect(jsonPath("$.status").value("PLANNING"))
                .andReturn().getResponse().getContentAsString();

        mvc.perform(get("/api/ideas").with(SUJAL))
                .andExpect(jsonPath("$[0].projectId").value(JsonPath.<Integer>read(project, "$.id")))
                .andExpect(jsonPath("$[0].projectTitle").value("Flux sampling benchmark"));
    }

    @Test
    void anIdeaBecomesAProjectOnlyOnce() throws Exception {
        long ideaId = idOf(postIdea("{\"title\": \"Twice\"}"));
        mvc.perform(post("/api/ideas/" + ideaId + "/promote").with(SUJAL).with(csrf()));

        mvc.perform(post("/api/ideas/" + ideaId + "/promote").with(SUJAL).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("This idea is already a project"));
    }

    @Test
    void promotingAnUnknownIdeaGives404() throws Exception {
        mvc.perform(post("/api/ideas/999999/promote").with(SUJAL).with(csrf()))
                .andExpect(status().isNotFound());
    }

    private ResultActions postIdea(String json) throws Exception {
        return mvc.perform(post("/api/ideas").with(SUJAL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static long idOf(ResultActions result) throws Exception {
        return ((Number) JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }
}

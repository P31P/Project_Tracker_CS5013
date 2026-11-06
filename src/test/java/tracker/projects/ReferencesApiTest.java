package tracker.projects;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static tracker.projects.ProjectApiTest.SUJAL;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import tracker.support.PostgresTestBase;

/** Archive papers and links as references, project upvotes and project discussions. */
@AutoConfigureMockMvc
@Transactional
class ReferencesApiTest extends PostgresTestBase {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void aPaperFromTheArchiveIsListedOnceAndCanBeRemoved() throws Exception {
        long project = project("Flux sampling");
        long paper = paper("Efficient flux sampling");

        send(post("/api/projects/" + project + "/papers"), "{\"paperId\": " + paper + "}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.papers[0].title").value("Efficient flux sampling"));
        send(post("/api/projects/" + project + "/papers"), "{\"paperId\": " + paper + "}")
                .andExpect(jsonPath("$.papers.length()").value(1));

        mvc.perform(delete("/api/projects/" + project + "/papers/" + paper).with(SUJAL).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.papers").isEmpty());
    }

    @Test
    void anUnknownPaperIsRefused() throws Exception {
        send(post("/api/projects/" + project("No paper") + "/papers"), "{\"paperId\": 999999}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("No such paper in the archive"));
    }

    @Test
    void votingOnAProjectCountsOncePerPerson() throws Exception {
        long project = project("Worth doing");
        mvc.perform(put("/api/projects/" + project + "/vote").with(SUJAL).with(csrf()));

        mvc.perform(put("/api/projects/" + project + "/vote").with(SUJAL).with(csrf()))
                .andExpect(jsonPath("$.voteCount").value(1))
                .andExpect(jsonPath("$.votedByMe").value(true));
        mvc.perform(get("/api/projects").with(SUJAL))
                .andExpect(jsonPath("$[0].voteCount").value(1));

        mvc.perform(delete("/api/projects/" + project + "/vote").with(SUJAL).with(csrf()))
                .andExpect(jsonPath("$.voteCount").value(0))
                .andExpect(jsonPath("$.votedByMe").value(false));
    }

    @Test
    void aProjectsDiscussionReadsOldestFirstAndIsCounted() throws Exception {
        long project = project("Discussed");
        send(post("/api/projects/" + project + "/comments"), "{\"body\": \"First\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorName").value("Sujal Jayant Kumar"));
        send(post("/api/projects/" + project + "/comments"), "{\"body\": \"Second\"}");

        mvc.perform(get("/api/projects/" + project + "/comments").with(SUJAL))
                .andExpect(jsonPath("$[0].body").value("First"))
                .andExpect(jsonPath("$[1].body").value("Second"));
        mvc.perform(get("/api/projects/" + project).with(SUJAL))
                .andExpect(jsonPath("$.commentCount").value(2));
    }

    @Test
    void anEmptyCommentIsRefused() throws Exception {
        send(post("/api/projects/" + project("Quiet") + "/comments"), "{\"body\": \"  \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("A comment can't be empty"));
    }

    @Test
    void anIdeaKeepsWebLinksAndPapers() throws Exception {
        long idea = idea("Read more about samplers");
        long paper = paper("A sampler survey");

        send(post("/api/ideas/" + idea + "/links"), "{\"url\": \"https://www.example.org/samplers\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.links[0].label").value("example.org"));
        send(post("/api/ideas/" + idea + "/papers"), "{\"paperId\": " + paper + "}")
                .andExpect(jsonPath("$.papers[0].title").value("A sampler survey"))
                .andExpect(jsonPath("$.links.length()").value(1));

        mvc.perform(get("/api/ideas").with(SUJAL))
                .andExpect(jsonPath("$[0].links[0].url").value("https://www.example.org/samplers"))
                .andExpect(jsonPath("$[0].papers[0].id").value(paper));
    }

    @Test
    void anIdeaLinkMustBeAWebAddressAndCanBeRemoved() throws Exception {
        long idea = idea("Links");
        send(post("/api/ideas/" + idea + "/links"), "{\"url\": \"javascript:alert(1)\"}")
                .andExpect(status().isBadRequest());

        String added = send(post("/api/ideas/" + idea + "/links"), "{\"url\": \"https://example.org\", \"label\": \"Docs\"}")
                .andReturn().getResponse().getContentAsString();
        long linkId = ((Number) JsonPath.read(added, "$.links[0].id")).longValue();

        mvc.perform(delete("/api/ideas/" + idea + "/links/" + linkId).with(SUJAL).with(csrf()))
                .andExpect(jsonPath("$.links").isEmpty());
    }

    @Test
    void addingAReferenceWithoutTheCsrfTokenIsRefused() throws Exception {
        mvc.perform(post("/api/projects/" + project("Guarded") + "/papers").with(SUJAL)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"paperId\": 1}"))
                .andExpect(status().isForbidden());
    }

    private long project(String title) throws Exception {
        String created = send(post("/api/projects"), "{\"title\": \"" + title + "\"}")
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(created, "$.id")).longValue();
    }

    private long idea(String title) throws Exception {
        String created = send(post("/api/ideas"), "{\"title\": \"" + title + "\"}")
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(created, "$.id")).longValue();
    }

    private long paper(String title) {
        return jdbc.sql("INSERT INTO paper (normalized_url, status, title) VALUES (:url, 'RESOLVED', :title) RETURNING id")
                .param("url", "https://example.org/" + title.replace(' ', '-')).param("title", title)
                .query(Long.class).single();
    }

    private ResultActions send(MockHttpServletRequestBuilder request, String json) throws Exception {
        return mvc.perform(request.with(SUJAL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json));
    }
}

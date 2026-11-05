package tracker.papers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tracker.data.Paper;
import tracker.data.PaperRepository;
import tracker.data.PaperSource;
import tracker.support.PostgresTestBase;

@AutoConfigureMockMvc
@Transactional
class PaperMarksApiTest extends PostgresTestBase {

    // both allowlisted by V3
    private static final RequestPostProcessor PRIYAL = oidcLogin().idToken(token -> token
            .claim("email", "cs24b047@smail.iitm.ac.in").claim("name", "Priyal Patel"));
    private static final RequestPostProcessor SUJAL = oidcLogin().idToken(token -> token
            .claim("email", "da24b026@smail.iitm.ac.in").claim("name", "Sujal Jayant Kumar"));

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PaperRepository papers;

    @Test
    void votingTwiceCountsOnce() throws Exception {
        long id = paper();
        mvc.perform(put("/api/papers/" + id + "/vote").with(PRIYAL).with(csrf()));

        mvc.perform(put("/api/papers/" + id + "/vote").with(PRIYAL).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voteCount").value(1))
                .andExpect(jsonPath("$.votedByMe").value(true));
    }

    @Test
    void votesFromTwoPeopleAddUpAndCanBeTakenBack() throws Exception {
        long id = paper();
        mvc.perform(put("/api/papers/" + id + "/vote").with(PRIYAL).with(csrf()));
        mvc.perform(put("/api/papers/" + id + "/vote").with(SUJAL).with(csrf()))
                .andExpect(jsonPath("$.voteCount").value(2));

        mvc.perform(delete("/api/papers/" + id + "/vote").with(PRIYAL).with(csrf()))
                .andExpect(jsonPath("$.voteCount").value(1))
                .andExpect(jsonPath("$.votedByMe").value(false));
        mvc.perform(delete("/api/papers/" + id + "/vote").with(PRIYAL).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voteCount").value(1));
    }

    @Test
    void readMarksArePrivate() throws Exception {
        long id = paper();

        mvc.perform(put("/api/papers/" + id + "/read").with(PRIYAL).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readByMe").value(true));

        mvc.perform(get("/api/papers/" + id).with(PRIYAL)).andExpect(jsonPath("$.readByMe").value(true));
        mvc.perform(get("/api/papers/" + id).with(SUJAL)).andExpect(jsonPath("$.readByMe").value(false));
    }

    @Test
    void aReadPaperCanBeMarkedUnreadAgain() throws Exception {
        long id = paper();
        mvc.perform(put("/api/papers/" + id + "/read").with(PRIYAL).with(csrf()));

        mvc.perform(delete("/api/papers/" + id + "/read").with(PRIYAL).with(csrf()))
                .andExpect(jsonPath("$.readByMe").value(false));
    }

    @Test
    void theListShowsVotesAndMyMarks() throws Exception {
        long id = paper();
        mvc.perform(put("/api/papers/" + id + "/vote").with(SUJAL).with(csrf()));
        mvc.perform(put("/api/papers/" + id + "/read").with(PRIYAL).with(csrf()));

        String path = "$[?(@.id == " + id + ")]";
        mvc.perform(get("/api/papers").with(PRIYAL))
                .andExpect(jsonPath(path + ".voteCount").value(1))
                .andExpect(jsonPath(path + ".votedByMe").value(false))
                .andExpect(jsonPath(path + ".readByMe").value(true));
    }

    @Test
    void tagsAreTidiedAndLeaveTheDetailsAlone() throws Exception {
        long id = paper();

        mvc.perform(put("/api/papers/" + id + "/tags").with(PRIYAL).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tags\": [\" #Genomics\", \"genomics\", \"Machine Learning\", \"\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags.length()").value(2))
                .andExpect(jsonPath("$.tags[0]").value("genomics"))
                .andExpect(jsonPath("$.tags[1]").value("machine learning"))
                .andExpect(jsonPath("$.source").value("REGISTRY"));
    }

    @Test
    void unknownPaperGets404() throws Exception {
        mvc.perform(put("/api/papers/999999/vote").with(PRIYAL).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No such paper"));
        mvc.perform(put("/api/papers/999999/read").with(PRIYAL).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void votingWithoutTheCsrfTokenIsRefused() throws Exception {
        mvc.perform(put("/api/papers/" + paper() + "/vote").with(PRIYAL)).andExpect(status().isForbidden());
    }

    private long paper() {
        Paper paper = new Paper("https://doi.org/10.1000/marks-test");
        paper.markResolved("A paper to vote on", List.of("A. Author"), null, "10.1000/marks-test",
                PaperSource.REGISTRY);
        return papers.saveAndFlush(paper).getId();
    }
}

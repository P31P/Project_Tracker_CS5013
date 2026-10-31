package tracker.papers;

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
import org.springframework.transaction.annotation.Transactional;
import tracker.data.Paper;
import tracker.data.PaperRepository;
import tracker.support.PostgresTestBase;

@AutoConfigureMockMvc
@Transactional
class PaperEditApiTest extends PostgresTestBase {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PaperRepository papers;

    @Test
    void fixingAnUnresolvedPaperResolvesItByHand() throws Exception {
        long id = unresolvedPaper();

        fix(id, "{\"title\": \"  Attention Is All You Need \", \"authors\": [\"Ashish Vaswani\", \" \", \" Noam Shazeer\"],"
                + " \"publishedOn\": \"2017-06-12\", \"doi\": \"\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.source").value("MANUAL"))
                .andExpect(jsonPath("$.title").value("Attention Is All You Need"))
                .andExpect(jsonPath("$.authors.length()").value(2))
                .andExpect(jsonPath("$.authors[1]").value("Noam Shazeer"))
                .andExpect(jsonPath("$.publishedOn").value("2017-06-12"))
                .andExpect(jsonPath("$.doi").isEmpty());

        mvc.perform(get("/api/papers/" + id).with(oidcLogin()))
                .andExpect(jsonPath("$.title").value("Attention Is All You Need"))
                .andExpect(jsonPath("$.source").value("MANUAL"));
    }

    @Test
    void fixingReplacesAllFourFields() throws Exception {
        long id = unresolvedPaper();
        fix(id, "{\"title\": \"First\", \"authors\": [\"A\"], \"publishedOn\": \"2020-01-01\", \"doi\": \"10.1/x\"}");

        fix(id, "{\"title\": \"Second\"}")
                .andExpect(jsonPath("$.title").value("Second"))
                .andExpect(jsonPath("$.authors").isEmpty())
                .andExpect(jsonPath("$.publishedOn").isEmpty())
                .andExpect(jsonPath("$.doi").isEmpty());
    }

    @Test
    void blankTitleIsRefusedWithAMessage() throws Exception {
        fix(unresolvedPaper(), "{\"title\": \"  \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("A paper needs a title"));
    }

    @Test
    void unknownPaperGets404() throws Exception {
        fix(999999L, "{\"title\": \"Anything\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No such paper"));
    }

    @Test
    void fixingWithoutTheCsrfTokenIsRefused() throws Exception {
        mvc.perform(patch("/api/papers/" + unresolvedPaper()).with(oidcLogin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\": \"No token\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void signedOutCallsGet401() throws Exception {
        mvc.perform(patch("/api/papers/1").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\": \"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    private long unresolvedPaper() {
        Paper paper = new Paper("https://example.com/fix-me.pdf");
        paper.markUnresolved();
        return papers.saveAndFlush(paper).getId();
    }

    private ResultActions fix(long id, String json) throws Exception {
        return mvc.perform(patch("/api/papers/" + id).with(oidcLogin()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }
}

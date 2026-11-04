package tracker.papers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tracker.data.Paper;
import tracker.data.PaperReference;
import tracker.data.PaperReferenceRepository;
import tracker.data.PaperRepository;
import tracker.support.PostgresTestBase;

@AutoConfigureMockMvc
@Transactional
class PaperReferencesApiTest extends PostgresTestBase {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PaperRepository papers;

    @Autowired
    private PaperReferenceRepository references;

    @Test
    void referencesComeInTheirOriginalOrder() throws Exception {
        Paper paper = papers.saveAndFlush(new Paper("https://doi.org/10.1/cited"));
        references.save(new PaperReference(paper, 2, "Second cited work", null));
        references.save(new PaperReference(paper, 1, "First cited work", "10.1/a"));

        mvc.perform(get("/api/papers/" + paper.getId() + "/references").with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].position").value(1))
                .andExpect(jsonPath("$[0].citation").value("First cited work"))
                .andExpect(jsonPath("$[0].doi").value("10.1/a"))
                .andExpect(jsonPath("$[1].doi").isEmpty());
    }

    @Test
    void paperWithoutReferencesGivesAnEmptyList() throws Exception {
        Paper paper = papers.saveAndFlush(new Paper("https://example.com/no-refs.pdf"));

        mvc.perform(get("/api/papers/" + paper.getId() + "/references").with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void unknownPaperGets404() throws Exception {
        mvc.perform(get("/api/papers/999999/references").with(oidcLogin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No such paper"));
    }

    @Test
    void signedOutCallsGet401() throws Exception {
        mvc.perform(get("/api/papers/1/references")).andExpect(status().isUnauthorized());
    }
}

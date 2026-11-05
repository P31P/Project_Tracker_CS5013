package tracker.papers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tracker.support.PostgresTestBase;

@AutoConfigureMockMvc
@Transactional
class PaperUsedInApiTest extends PostgresTestBase {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void listsTheProjectsThenTheIdeasThatReferenceThePaper() throws Exception {
        long paper = insert("INSERT INTO paper (normalized_url, status, title) VALUES ('https://doi.org/10.1/used', "
                + "'RESOLVED', 'A used paper') RETURNING id");
        long project = insert("INSERT INTO project (title) VALUES ('Zebra project') RETURNING id");
        long idea = insert("INSERT INTO idea (title) VALUES ('An idea') RETURNING id");
        jdbc.sql("INSERT INTO project_paper (project_id, paper_id) VALUES (:p, :paper)")
                .param("p", project).param("paper", paper).update();
        jdbc.sql("INSERT INTO idea_paper (idea_id, paper_id) VALUES (:i, :paper)")
                .param("i", idea).param("paper", paper).update();

        mvc.perform(get("/api/papers/" + paper + "/used-in").with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("PROJECT"))
                .andExpect(jsonPath("$[0].title").value("Zebra project"))
                .andExpect(jsonPath("$[1].type").value("IDEA"))
                .andExpect(jsonPath("$[1].id").value(idea));
    }

    @Test
    void anUnusedPaperHasAnEmptyListAndAnUnknownOneIs404() throws Exception {
        long paper = insert("INSERT INTO paper (normalized_url, status) VALUES ('https://doi.org/10.1/unused', "
                + "'PENDING') RETURNING id");

        mvc.perform(get("/api/papers/" + paper + "/used-in").with(oidcLogin()))
                .andExpect(jsonPath("$").isEmpty());
        mvc.perform(get("/api/papers/999999/used-in").with(oidcLogin()))
                .andExpect(status().isNotFound());
    }

    private long insert(String sql) {
        return jdbc.sql(sql).query(Long.class).single();
    }
}

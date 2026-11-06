package tracker.search;

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
class SearchControllerTest extends PostgresTestBase {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void returnsMatchesWithTheirType() throws Exception {
        jdbc.sql("INSERT INTO idea (title) VALUES ('Quantum annealing')").update();

        mvc.perform(get("/api/search").param("q", "annealing").with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("IDEA"))
                .andExpect(jsonPath("$[0].title").value("Quantum annealing"));
    }

    @Test
    void typeFilterIsApplied() throws Exception {
        jdbc.sql("INSERT INTO idea (title) VALUES ('Quantum annealing')").update();

        mvc.perform(get("/api/search").param("q", "annealing").param("type", "PROJECT").with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void missingQueryGivesAnEmptyList() throws Exception {
        mvc.perform(get("/api/search").with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void unknownTypeIsRefused() throws Exception {
        mvc.perform(get("/api/search").param("q", "x").param("type", "BOOK").with(oidcLogin()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signedOutCallsGet401() throws Exception {
        mvc.perform(get("/api/search").param("q", "x"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void repeatedTagParametersMustAllMatch() throws Exception {
        jdbc.sql("INSERT INTO idea (title, tags) VALUES ('Both tags', '{graphs,genomics}')").update();
        jdbc.sql("INSERT INTO idea (title, tags) VALUES ('One tag', '{graphs}')").update();

        mvc.perform(get("/api/search").param("tag", "Graphs").param("tag", "#genomics").with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Both tags"))
                .andExpect(jsonPath("$[0].tags[0]").value("graphs"));
    }

    @Test
    void badDateIsRefusedWithAMessage() throws Exception {
        mvc.perform(get("/api/search").param("from", "5/11/2026").with(oidcLogin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Dates look like 2026-11-05, not 5/11/2026"));
    }

    @Test
    void startAfterEndIsRefused() throws Exception {
        mvc.perform(get("/api/search").param("from", "2026-11-05").param("to", "2026-11-01").with(oidcLogin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("The start date is after the end date"));
    }
}

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
}

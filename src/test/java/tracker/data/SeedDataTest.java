package tracker.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tracker.support.PostgresTestBase;

/**
 * Loads {@code src/test/resources/seed-mock-data.sql} into the test database (rolled back afterwards), so the
 * script keeps working as the schema changes.
 */
@AutoConfigureMockMvc
@Transactional
class SeedDataTest extends PostgresTestBase {

    private static final List<String> TABLES = List.of("app_user", "project", "project_member", "project_link",
            "project_update", "idea", "idea_vote", "idea_comment");

    // allowlisted by V3 and the owner of several seeded projects
    private static final RequestPostProcessor TEAM_MEMBER = oidcLogin().idToken(token -> token
            .claim("email", "da24b026@smail.iitm.ac.in")
            .claim("name", "Sujal Jayant Kumar"));

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private MockMvc mvc;

    @Test
    void scriptFillsEveryTable() {
        Map<String, Integer> before = counts();

        loadSeed();

        Map<String, Integer> added = new LinkedHashMap<>();
        counts().forEach((table, count) -> added.put(table, count - before.get(table)));
        assertThat(added).isEqualTo(Map.of(
                "app_user", 4, "project", 16, "project_member", 28, "project_link", 5,
                "project_update", 12, "idea", 12, "idea_vote", 15, "idea_comment", 5));
    }

    @Test
    void seededDataShowsUpInTheApi() throws Exception {
        loadSeed();

        mvc.perform(get("/api/overview").with(TEAM_MEMBER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mine.length()").value(5));
        mvc.perform(get("/api/search").param("q", "flux sampling").with(TEAM_MEMBER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.type == 'PROJECT')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.type == 'IDEA')]").isNotEmpty());
    }

    @Test
    void runningTheScriptTwiceFails() {
        loadSeed();

        assertThatThrownBy(this::loadSeed).hasStackTraceContaining("app_user_email_key");
    }

    // runs on the test's own connection, so the rows roll back with the test
    private void loadSeed() {
        ScriptUtils.executeSqlScript(DataSourceUtils.getConnection(dataSource),
                new EncodedResource(new ClassPathResource("seed-mock-data.sql"), StandardCharsets.UTF_8));
    }

    private Map<String, Integer> counts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String table : TABLES) {
            counts.put(table, jdbc.sql("SELECT count(*) FROM " + table).query(Integer.class).single());
        }
        return counts;
    }
}

package tracker.demo;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tracker.support.PostgresTestBase;

@AutoConfigureMockMvc
@ActiveProfiles("demo")
@Transactional
class DemoModeTest extends PostgresTestBase {

    // the shared test database isn't the bundled demo one, so these tests migrate it the usual way
    @TestConfiguration
    static class PlainMigration {

        @Bean
        @Primary
        FlywayMigrationStrategy plainMigration() {
            return Flyway::migrate;
        }
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private DataSource dataSource;

    @Test
    void theSignInPageIsToldToOfferTheDemoSignIn() throws Exception {
        mvc.perform(get("/demo/available")).andExpect(status().isNoContent());
    }

    @Test
    void aGraderSignsInAsTheDemoMember() throws Exception {
        jdbc.sql("INSERT INTO app_user (email, name, allowlisted) VALUES ('demo@example.com', 'Demo User', true)")
                .update();

        MockHttpSession session = (MockHttpSession) mvc.perform(get("/demo/sign-in"))
                .andExpect(redirectedUrl("/"))
                .andReturn().getRequest().getSession(false);

        mvc.perform(get("/api/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("demo@example.com"))
                .andExpect(jsonPath("$.role").value("MEMBER"));
    }

    @Test
    void theRestOfTheAppStillNeedsSignIn() throws Exception {
        mvc.perform(get("/api/projects")).andExpect(status().isUnauthorized());
        mvc.perform(get("/")).andExpect(redirectedUrl("/login.html"));
    }

    @Test
    void demoModeRefusesADatabaseWithoutTheMarker() {
        assertThatThrownBy(() -> DemoDatabase.requireDemoDatabase(dataSource))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("demo.marker");
    }
}

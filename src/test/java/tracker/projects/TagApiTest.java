package tracker.projects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tracker.support.PostgresTestBase;

@AutoConfigureMockMvc
@Transactional
class TagApiTest extends PostgresTestBase {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void builtInTagsComeInTheLabsGroupsAndOrder() throws Exception {
        mvc.perform(get("/api/tags").with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groups[*].label",
                        Matchers.contains("Broad direction", "Methodology", "Other")))
                .andExpect(jsonPath("$.groups[0].tags[0].name").value("bioinformatics"))
                .andExpect(jsonPath("$.groups[0].tags[0].label").value("Bioinformatics"))
                .andExpect(jsonPath("$.groups[1].tags.length()").value(14))
                .andExpect(jsonPath("$.groups[2].tags[1].label").value("Open Source"));
    }

    @Test
    void customTagsInUseAreListedOnceMostUsedFirst() throws Exception {
        jdbc.sql("INSERT INTO project (title, tags) VALUES ('A', '{graphs,genomics}')").update();
        jdbc.sql("INSERT INTO idea (title, tags) VALUES ('B', '{graphs,flux sampling}')").update();
        jdbc.sql("UPDATE app_user SET interests = '{graphs}' WHERE email = 'cs24b047@smail.iitm.ac.in'").update();

        mvc.perform(get("/api/tags").with(oidcLogin()))
                .andExpect(jsonPath("$.custom[0]").value("graphs"))
                .andExpect(jsonPath("$.custom", Matchers.hasItem("flux sampling")))
                .andExpect(jsonPath("$.custom", Matchers.not(Matchers.hasItem("genomics"))));
    }

    @Test
    void storedNamesAreTidiedLikeEveryOtherTag() {
        Long untidy = jdbc.sql("SELECT count(*) FROM tag WHERE name <> lower(btrim(name))").query(Long.class).single();
        assertThat(untidy).isZero();
    }

    @Test
    void signedOutCallsGet401() throws Exception {
        mvc.perform(get("/api/tags")).andExpect(status().isUnauthorized());
    }
}

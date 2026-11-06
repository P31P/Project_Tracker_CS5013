package tracker.people;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tracker.support.PostgresTestBase;

@AutoConfigureMockMvc
@Transactional
class DirectoryApiTest extends PostgresTestBase {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcClient jdbc;

    private long priyal;

    @BeforeEach
    void setUp() {
        priyal = jdbc.sql("SELECT id FROM app_user WHERE email = 'cs24b047@smail.iitm.ac.in'").query(Long.class).single();
        jdbc.sql("UPDATE app_user SET background = 'BTech, DSAI', interests = '{genomics}' WHERE id = :id")
                .param("id", priyal).update();
    }

    @Test
    void listsOnlyPeopleWhoCanSignIn() throws Exception {
        jdbc.sql("INSERT INTO app_user (email, name, allowlisted) VALUES ('gone@example.com', 'Gone', false)").update();

        mvc.perform(get("/api/people/directory").with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Gone')]").isEmpty())
                .andExpect(jsonPath("$[?(@.name == 'Priyal Patel')].background").value("BTech, DSAI"))
                .andExpect(jsonPath("$[?(@.name == 'Priyal Patel')].interests[0]").value("genomics"));
    }

    @Test
    void countsOwnedAndJoinedProjectsAndIdeas() throws Exception {
        long sujal = jdbc.sql("SELECT id FROM app_user WHERE email = 'da24b026@smail.iitm.ac.in'")
                .query(Long.class).single();
        jdbc.sql("INSERT INTO project (title, owner_id) VALUES ('Mine', :me)").param("me", priyal).update();
        long joined = jdbc.sql("INSERT INTO project (title, owner_id) VALUES ('Joined', :other) RETURNING id")
                .param("other", sujal).query(Long.class).single();
        jdbc.sql("INSERT INTO project_member (project_id, user_id) VALUES (:p, :me)")
                .param("p", joined).param("me", priyal).update();
        jdbc.sql("INSERT INTO idea (title, author_id) VALUES ('An idea', :me)").param("me", priyal).update();

        String me = "$[?(@.name == 'Priyal Patel')]";
        mvc.perform(get("/api/people/directory").with(oidcLogin()))
                .andExpect(jsonPath(me + ".projectCount").value(2))
                .andExpect(jsonPath(me + ".ideaCount").value(1));
    }

    @Test
    void aPersonsPageHasTheirProfileProjectsAndIdeas() throws Exception {
        jdbc.sql("INSERT INTO project (title, owner_id) VALUES ('Genome graphs', :me)").param("me", priyal).update();
        jdbc.sql("INSERT INTO idea (title, author_id) VALUES ('Read clustering', :me)").param("me", priyal).update();

        mvc.perform(get("/api/people/" + priyal).with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.name").value("Priyal Patel"))
                .andExpect(jsonPath("$.profile.interests[0]").value("genomics"))
                .andExpect(jsonPath("$.projects[0].title").value("Genome graphs"))
                .andExpect(jsonPath("$.ideas[0].title").value("Read clustering"));
    }

    @Test
    void unknownOrRemovedPeopleGive404() throws Exception {
        long gone = jdbc.sql("INSERT INTO app_user (email, name, allowlisted) VALUES ('gone@example.com', 'Gone', false) "
                + "RETURNING id").query(Long.class).single();

        mvc.perform(get("/api/people/999999").with(oidcLogin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No such person"));
        mvc.perform(get("/api/people/" + gone).with(oidcLogin())).andExpect(status().isNotFound());
    }

    @Test
    void theMemberPickerListStillWorks() throws Exception {
        mvc.perform(get("/api/people").with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Priyal Patel')]").isNotEmpty());
    }

    @Test
    void signedOutCallsGet401() throws Exception {
        mvc.perform(get("/api/people/directory")).andExpect(status().isUnauthorized());
    }
}

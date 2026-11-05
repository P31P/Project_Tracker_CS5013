package tracker.profile;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static tracker.profile.ProfileApiTest.PRIYAL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tracker.support.PostgresTestBase;

/** "For you" and "Your ideas" on the Home tab, as returned by {@code GET /api/overview}. */
@AutoConfigureMockMvc
@Transactional
class ForYouApiTest extends PostgresTestBase {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcClient jdbc;

    private long priyal;
    private long sujal;

    @BeforeEach
    void users() {
        priyal = userId("cs24b047@smail.iitm.ac.in");
        sujal = userId("da24b026@smail.iitm.ac.in");
    }

    @Test
    void someoneElsesIdeaWithMyInterestShowsUp() throws Exception {
        interestedIn("genomics");
        idea("Long-read genome assembly", "{genomics}", sujal, "now()");

        overview()
                .andExpect(jsonPath("$.forYou[0].kind").value("IDEA"))
                .andExpect(jsonPath("$.forYou[0].who").value("Sujal Jayant Kumar"))
                .andExpect(jsonPath("$.forYou[0].target").value("Long-read genome assembly"))
                .andExpect(jsonPath("$.forYou[0].interest").value("genomics"));
    }

    @Test
    void myOwnIdeasOldIdeasAndOtherTopicsDontShowUp() throws Exception {
        interestedIn("genomics");
        idea("My own genomics idea", "{genomics}", priyal, "now()");
        idea("Old genomics idea", "{genomics}", sujal, "now() - interval '40 days'");
        idea("A robotics idea", "{robotics}", sujal, "now()");

        overview().andExpect(jsonPath("$.forYou").value(empty()));
    }

    @Test
    void aPaperMatchesByTagOrByATitlePhraseUntilIRead() throws Exception {
        interestedIn("machine learning", "genomics");
        long titled = paper("https://doi.org/10.1/a", "Machine learning for flux analysis", "{}");
        paper("https://doi.org/10.1/b", "Assembling reads", "{genomics}");
        paper("https://doi.org/10.1/c", "Machine vision and learning", "{}");

        overview()
                .andExpect(jsonPath("$.forYou[*].target",
                        hasItem("Machine learning for flux analysis")))
                .andExpect(jsonPath("$.forYou[*].target", hasItem("Assembling reads")))
                .andExpect(jsonPath("$.forYou[*].target", not(hasItem("Machine vision and learning"))));

        mvc.perform(put("/api/papers/" + titled + "/read").with(PRIYAL).with(csrf()));

        overview().andExpect(jsonPath("$.forYou[*].target", contains("Assembling reads")));
    }

    @Test
    void withoutInterestsThereIsNothingForYou() throws Exception {
        idea("Long-read genome assembly", "{genomics}", sujal, "now()");

        overview().andExpect(jsonPath("$.forYou").value(empty()));
    }

    @Test
    void myIdeasAreListedNewestFirst() throws Exception {
        idea("First idea", "{}", priyal, "now() - interval '1 day'");
        idea("Second idea", "{}", priyal, "now()");
        idea("Not mine", "{}", sujal, "now()");

        overview()
                .andExpect(jsonPath("$.myIdeas[*].title", contains("Second idea", "First idea")))
                .andExpect(jsonPath("$.myIdeas[0].voteCount").value(0));
    }

    private void interestedIn(String... interests) throws Exception {
        mvc.perform(patch("/api/profile").with(PRIYAL).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"interests\": [\"" + String.join("\", \"", interests) + "\"]}"));
    }

    // createdAt is SQL, such as now() - interval '40 days'
    private void idea(String title, String tags, long authorId, String createdAt) {
        jdbc.sql("INSERT INTO idea (title, tags, author_id, created_at) VALUES (:title, CAST(:tags AS text[]), :author, "
                        + createdAt + ")")
                .param("title", title).param("tags", tags).param("author", authorId).update();
    }

    private long paper(String url, String title, String tags) {
        return jdbc.sql("INSERT INTO paper (normalized_url, status, title, tags) "
                        + "VALUES (:url, 'RESOLVED', :title, CAST(:tags AS text[])) RETURNING id")
                .param("url", url).param("title", title).param("tags", tags).query(Long.class).single();
    }

    private ResultActions overview() throws Exception {
        return mvc.perform(get("/api/overview").with(PRIYAL));
    }

    private long userId(String email) {
        return jdbc.sql("SELECT id FROM app_user WHERE email = :email").param("email", email).query(Long.class).single();
    }
}

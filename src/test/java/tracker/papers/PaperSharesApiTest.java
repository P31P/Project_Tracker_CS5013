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
import tracker.data.PaperRepository;
import tracker.data.PaperShare;
import tracker.data.PaperShareRepository;
import tracker.data.SlackMessage;
import tracker.data.SlackMessageRepository;
import tracker.support.PostgresTestBase;

@AutoConfigureMockMvc
@Transactional
class PaperSharesApiTest extends PostgresTestBase {

    private static final String URL = "https://arxiv.org/abs/1706.03762";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PaperRepository papers;

    @Autowired
    private SlackMessageRepository messages;

    @Autowired
    private PaperShareRepository shares;

    @Test
    void listIsEmptyWhenThereAreNoPapers() throws Exception {
        mvc.perform(get("/api/papers").with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void paperShowsWhoSharedItFirstAndWhere() throws Exception {
        Paper paper = papers.saveAndFlush(new Paper(URL));
        share(paper, "1696412345.000100", "Priyal Patel", "https://lab.slack.com/archives/C1/p1");
        share(paper, "1696412999.000200", "Sujal Kumar", "https://lab.slack.com/archives/C1/p2");

        mvc.perform(get("/api/papers/" + paper.getId()).with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sharedBy").value("Priyal Patel"))
                .andExpect(jsonPath("$.slackPermalink").value("https://lab.slack.com/archives/C1/p1"))
                .andExpect(jsonPath("$.sharedAt").isNotEmpty());
    }

    @Test
    void listShowsTheShareOfEachPaper() throws Exception {
        Paper first = papers.saveAndFlush(new Paper(URL));
        Paper second = papers.saveAndFlush(new Paper("https://doi.org/10.1000/xyz"));
        share(first, "1696412345.000100", "Priyal Patel", "https://lab.slack.com/archives/C1/p1");
        share(second, "1696412999.000200", "Sujal Kumar", "https://lab.slack.com/archives/C1/p2");

        mvc.perform(get("/api/papers").with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.normalizedUrl=='" + URL + "')].sharedBy").value("Priyal Patel"))
                .andExpect(jsonPath("$[?(@.normalizedUrl=='https://doi.org/10.1000/xyz')].sharedBy")
                        .value("Sujal Kumar"));
    }

    @Test
    void paperWithoutAShareHasNoSlackFields() throws Exception {
        Paper paper = papers.saveAndFlush(new Paper(URL));

        mvc.perform(get("/api/papers/" + paper.getId()).with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sharedBy").doesNotExist())
                .andExpect(jsonPath("$.slackPermalink").doesNotExist());
    }

    private void share(Paper paper, String ts, String userName, String permalink) {
        SlackMessage message = new SlackMessage("C1", ts, "U1", "<@UBOT> " + paper.getNormalizedUrl());
        message.setSlackUserName(userName);
        message.setPermalink(permalink);
        messages.saveAndFlush(message);
        shares.saveAndFlush(new PaperShare(paper, message, paper.getNormalizedUrl()));
    }
}

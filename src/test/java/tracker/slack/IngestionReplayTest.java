package tracker.slack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.slack.api.RequestConfigurator;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.response.chat.ChatGetPermalinkResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import tracker.data.PaperRepository;
import tracker.data.PaperShareRepository;
import tracker.data.SlackMessageRepository;
import tracker.support.PostgresTestBase;

@Transactional
class IngestionReplayTest extends PostgresTestBase {

    @Autowired
    private IngestionService ingestion;

    @Autowired
    private SlackMessageRepository messages;

    @Autowired
    private PaperRepository papers;

    @Autowired
    private PaperShareRepository shares;

    private MethodsClient client;

    @BeforeEach
    void stubClient() throws Exception {
        client = mock(MethodsClient.class);
        ChatGetPermalinkResponse resp = new ChatGetPermalinkResponse();
        resp.setOk(true);
        resp.setPermalink("https://ramanlab.slack.com/archives/C123/p1696412345000100");
        when(client.chatGetPermalink(any(RequestConfigurator.class))).thenReturn(resp);
    }

    @Test
    void replayedEventCreatesNoDuplicates() {
        String channelId = "C123";
        String messageTs = "1696412345.000100";
        String userId = "U123";
        String text = "<@UBOT> check this out <https://arxiv.org/abs/1706.03762|Attention>";

        ingestion.ingest(channelId, messageTs, userId, text, client);
        ingestion.ingest(channelId, messageTs, userId, text, client);

        assertThat(messages.count()).isEqualTo(1);
        assertThat(papers.count()).isEqualTo(1);
        assertThat(shares.count()).isEqualTo(1);
    }

    @Test
    void messageWithNoPaperLinksCreatesNoShares() {
        ingestion.ingest("C123", "1696412345.000200", "U123", "<@UBOT> lunch at 1?", client);

        assertThat(messages.count()).isEqualTo(1);
        assertThat(papers.count()).isEqualTo(0);
        assertThat(shares.count()).isEqualTo(0);
    }

    @Test
    void multipleLinksInOneMessageCreateSeparatePapers() {
        String text = "<@UBOT> two papers: "
                + "<https://arxiv.org/abs/1706.03762|A> "
                + "<https://doi.org/10.1145/3292500.3330701|B>";

        ingestion.ingest("C123", "1696412345.000300", "U123", text, client);

        assertThat(papers.count()).isEqualTo(2);
        assertThat(shares.count()).isEqualTo(2);
    }
}

package tracker.slack;

import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.chat.ChatGetPermalinkResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tracker.data.Paper;
import tracker.data.PaperRepository;
import tracker.data.PaperShare;
import tracker.data.PaperShareRepository;
import tracker.data.SlackMessage;
import tracker.data.SlackMessageRepository;

@Service
public class IngestionService {

    private final SlackMessageRepository messages;
    private final PaperRepository papers;
    private final PaperShareRepository shares;
    private final LinkExtractor extractor;

    public IngestionService(SlackMessageRepository messages, PaperRepository papers,
                            PaperShareRepository shares, LinkExtractor extractor) {
        this.messages = messages;
        this.papers = papers;
        this.shares = shares;
        this.extractor = extractor;
    }

    /** Idempotent ingestion — safe to call twice with the same event. */
    @Transactional
    public void ingest(String channelId, String messageTs, String userId, String text,
                       MethodsClient client) {
        doIngest(channelId, messageTs, userId, text, client);
    }

    /** Same as {@link #ingest} but runs on a background thread; called from the Slack event handler. */
    @Async
    @Transactional
    public void ingestAsync(String channelId, String messageTs, String userId, String text,
                            MethodsClient client) {
        doIngest(channelId, messageTs, userId, text, client);
    }

    private void doIngest(String channelId, String messageTs, String userId, String text,
                          MethodsClient client) {
        if (messages.findByChannelIdAndMessageTs(channelId, messageTs).isPresent()) {
            return;
        }

        SlackMessage message = messages.saveAndFlush(
                new SlackMessage(channelId, messageTs, userId, text));

        try {
            ChatGetPermalinkResponse resp = client.chatGetPermalink(
                    r -> r.channel(channelId).messageTs(messageTs));
            if (resp.isOk()) {
                message.setPermalink(resp.getPermalink());
                messages.save(message);
            }
        } catch (SlackApiException | IOException e) {
            // permalink is optional; carry on
        }

        List<PaperLink> links = extractor.extract(text);
        for (PaperLink link : links) {
            Paper paper = papers.findByNormalizedUrl(link.normalizedUrl())
                    .orElseGet(() -> papers.saveAndFlush(new Paper(link.normalizedUrl())));
            shares.save(new PaperShare(paper, message, link.url()));
        }
    }
}

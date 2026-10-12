package tracker.slack;

import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.chat.ChatGetPermalinkResponse;
import com.slack.api.methods.response.users.UsersInfoResponse;
import com.slack.api.model.User;
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

/** Stores Slack messages that mention the bot, and the papers linked in them. */
@Service
public class IngestionService {

    private final SlackMessageRepository messages;
    private final PaperRepository papers;
    private final PaperShareRepository shares;
    private final LinkExtractor extractor;

    /**
     * Creates the service.
     *
     * @param messages where Slack messages are stored
     * @param papers where papers are stored
     * @param shares where shares are stored
     * @param extractor finds paper links in message text
     */
    public IngestionService(SlackMessageRepository messages, PaperRepository papers,
                            PaperShareRepository shares, LinkExtractor extractor) {
        this.messages = messages;
        this.papers = papers;
        this.shares = shares;
        this.extractor = extractor;
    }

    /**
     * Stores a message and its paper links. Safe to call twice with the same event: a message already stored is
     * skipped.
     *
     * @param channelId the Slack channel id
     * @param messageTs the message timestamp
     * @param userId the poster's Slack user id
     * @param text the message text
     * @param client the Slack client, used to fetch the permalink and the poster's name
     */
    @Transactional
    public void ingest(String channelId, String messageTs, String userId, String text,
                       MethodsClient client) {
        doIngest(channelId, messageTs, userId, text, client);
    }

    /**
     * Same as {@link #ingest} but runs on a background thread, so the Slack event can be acknowledged at once.
     *
     * @param channelId the Slack channel id
     * @param messageTs the message timestamp
     * @param userId the poster's Slack user id
     * @param text the message text
     * @param client the Slack client, used to fetch the permalink and the poster's name
     */
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

        try {
            UsersInfoResponse resp = client.usersInfo(r -> r.user(userId));
            if (resp.isOk() && resp.getUser() != null) {
                User user = resp.getUser();
                boolean hasRealName = user.getRealName() != null && !user.getRealName().isBlank();
                message.setSlackUserName(hasRealName ? user.getRealName() : user.getName());
                messages.save(message);
            }
        } catch (SlackApiException | IOException e) {
            // the poster's name is optional (needs the users:read scope); carry on
        }

        List<PaperLink> links = extractor.extract(text);
        for (PaperLink link : links) {
            Paper paper = papers.findByNormalizedUrl(link.normalizedUrl())
                    .orElseGet(() -> papers.saveAndFlush(new Paper(link.normalizedUrl())));
            shares.save(new PaperShare(paper, message, link.url()));
        }
    }
}

package tracker.slack;

import com.slack.api.bolt.App;
import com.slack.api.model.event.AppMentionEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Listens for messages that mention the bot. Runs only when {@code slack.socket-mode-enabled} is {@code true}. */
@Component
@ConditionalOnProperty(name = "slack.socket-mode-enabled", havingValue = "true")
public class MessageEventHandler {

    /**
     * Registers the handler. Messages from bots and edited or deleted messages are ignored; the rest are stored in
     * the background and acknowledged at once.
     *
     * @param app the Slack app to listen on
     * @param ingestion stores the messages
     */
    public MessageEventHandler(App app, IngestionService ingestion) {
        app.event(AppMentionEvent.class, (payload, ctx) -> {
            AppMentionEvent event = payload.getEvent();
            // Skip bot messages and edited/deleted subtypes
            if (event.getBotId() != null || event.getSubtype() != null) {
                return ctx.ack();
            }
            ingestion.ingestAsync(
                    event.getChannel(), event.getTs(), event.getUser(), event.getText(),
                    ctx.client());
            return ctx.ack();
        });
    }
}

package tracker.slack;

import com.slack.api.bolt.App;
import com.slack.api.model.event.AppMentionEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "slack.socket-mode-enabled", havingValue = "true")
public class MessageEventHandler {

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

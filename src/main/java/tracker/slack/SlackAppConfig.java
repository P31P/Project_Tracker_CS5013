package tracker.slack;

import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.jakarta_socket_mode.SocketModeApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Connects to Slack over Socket Mode. Runs only when {@code slack.socket-mode-enabled} is {@code true}. */
@Configuration
@ConditionalOnProperty(name = "slack.socket-mode-enabled", havingValue = "true")
public class SlackAppConfig {

    private static final Logger log = LoggerFactory.getLogger(SlackAppConfig.class);

    /**
     * Creates the Slack app.
     *
     * @param botToken the bot token, from {@code slack.bot-token}
     * @param signingSecret the signing secret, from {@code slack.signing-secret}
     * @return the app
     */
    @Bean
    App slackApp(
            @Value("${slack.bot-token}") String botToken,
            @Value("${slack.signing-secret}") String signingSecret) {
        return new App(AppConfig.builder()
                .singleTeamBotToken(botToken)
                .signingSecret(signingSecret)
                .build());
    }

    /**
     * Opens the Socket Mode connection when the app starts and closes it when the app stops. If Slack can't be
     * reached, the error is logged and the rest of the tracker keeps running.
     *
     * @param app the Slack app
     * @param appToken the app-level token, from {@code slack.app-token}
     * @return the lifecycle Spring starts and stops
     */
    @Bean
    SmartLifecycle socketModeLifecycle(
            App app,
            @Value("${slack.app-token}") String appToken) {
        return new SmartLifecycle() {
            private SocketModeApp socketModeApp;
            private volatile boolean running = false;

            @Override
            public void start() {
                try {
                    socketModeApp = new SocketModeApp(appToken, app);
                    socketModeApp.startAsync();
                    running = true;
                } catch (Exception e) {
                    // a Slack problem should pause paper capture, not stop the whole tracker
                    log.error("Slack ingestion disabled: {}", e.getMessage());
                }
            }

            @Override
            public void stop() {
                if (socketModeApp != null) {
                    try {
                        socketModeApp.stop();
                    } catch (Exception ignored) {
                    }
                }
                running = false;
            }

            @Override
            public boolean isRunning() {
                return running;
            }
        };
    }
}

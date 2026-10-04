package tracker.slack;

import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.jakarta_socket_mode.SocketModeApp;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "slack.socket-mode-enabled", havingValue = "true")
public class SlackAppConfig {

    @Bean
    App slackApp(
            @Value("${slack.bot-token}") String botToken,
            @Value("${slack.signing-secret}") String signingSecret) {
        return new App(AppConfig.builder()
                .singleTeamBotToken(botToken)
                .signingSecret(signingSecret)
                .build());
    }

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
                    throw new RuntimeException("Failed to start Socket Mode", e);
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

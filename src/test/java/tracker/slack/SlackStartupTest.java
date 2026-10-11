package tracker.slack;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import tracker.support.PostgresTestBase;

@TestPropertySource(properties = {
        "slack.socket-mode-enabled=true",
        "slack.bot-token=xoxb-invalid",
        "slack.app-token=xapp-invalid"
})
class SlackStartupTest extends PostgresTestBase {

    // Passes only if the app finishes starting even though Slack rejects the tokens
    @Test
    void appStartsWhenSlackCannotConnect() {
    }
}

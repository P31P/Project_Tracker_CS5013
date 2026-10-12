package tracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Entry point of the Raman Lab tracker: a Spring Boot app for lab projects, ideas and the papers shared in Slack.
 */
@SpringBootApplication
@EnableAsync
public class TrackerApplication {

    /**
     * Starts the application.
     *
     * @param args command-line arguments, passed on to Spring Boot
     */
    public static void main(String[] args) {
        SpringApplication.run(TrackerApplication.class, args);
    }
}

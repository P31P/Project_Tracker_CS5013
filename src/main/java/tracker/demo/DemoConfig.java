package tracker.demo;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Demo mode, for the Docker image graders run: a "Continue as demo user" sign-in instead of Google, on the
 * database bundled in the image. It exists only in the {@code demo} profile, which only the Dockerfile turns on;
 * without it none of this is created and sign-in works as usual.
 */
@Configuration
@Profile("demo")
public class DemoConfig {

    private static final Logger log = LoggerFactory.getLogger(DemoConfig.class);

    /**
     * Checks the database, migrates it and loads the demo data once, instead of the plain migration.
     *
     * @param seedFiles SQL files to load on the first start, in order ({@code demo.seed-files})
     * @return the migration strategy Spring Boot runs at start-up
     */
    @Bean
    FlywayMigrationStrategy demoDatabase(@Value("${demo.seed-files}") List<String> seedFiles) {
        log.warn("Demo mode is on: anyone who opens the sign-in page can enter as the demo user. "
                + "It only starts on the database bundled in the Docker image.");
        return flyway -> DemoDatabase.prepare(flyway, seedFiles);
    }

    /**
     * Opens {@code /demo/**} to signed-out visitors; every other path keeps the rules in
     * {@link tracker.auth.SecurityConfig}.
     *
     * @param http Spring Security's builder
     * @return the filter chain for the demo sign-in, checked before the main one
     * @throws Exception if Spring Security cannot build the chain
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    SecurityFilterChain demoSignInChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/demo/**")
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll());
        return http.build();
    }
}

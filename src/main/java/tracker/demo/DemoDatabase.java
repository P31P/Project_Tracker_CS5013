package tracker.demo;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/**
 * Gets the database bundled in the Docker image ready for demo mode: it refuses any other database, runs the
 * migrations, and loads the demo data on the first start.
 */
final class DemoDatabase {

    private static final Logger log = LoggerFactory.getLogger(DemoDatabase.class);

    private DemoDatabase() {
    }

    /**
     * Checks that this is the bundled demo database, migrates it, and loads the demo data once. Runs in place of
     * Spring Boot's plain migration.
     *
     * @param flyway the app's Flyway, with its data source
     * @param seedFiles SQL files to load on the first start, in order
     * @throws IllegalStateException if the database isn't the bundled demo one, or the demo data can't be loaded
     */
    static void prepare(Flyway flyway, List<String> seedFiles) {
        DataSource dataSource = flyway.getConfiguration().getDataSource();
        requireDemoDatabase(dataSource);
        flyway.migrate();
        seedOnce(dataSource, seedFiles);
    }

    /**
     * Refuses to go on unless the database has the {@code demo.marker} table that only the Docker image's start
     * script creates, so the demo sign-in can never open the lab's real data.
     *
     * @param dataSource the app's database
     * @throws IllegalStateException if the marker table is missing
     */
    static void requireDemoDatabase(DataSource dataSource) {
        Boolean bundled = new JdbcTemplate(dataSource)
                .queryForObject("SELECT to_regclass('demo.marker') IS NOT NULL", Boolean.class);
        if (!Boolean.TRUE.equals(bundled)) {
            throw new IllegalStateException("Demo mode only runs on the database bundled in the Docker image "
                    + "(it has a demo.marker table), so the demo sign-in never opens real data. Refusing to start.");
        }
    }

    // all files in one transaction, so a failed load leaves the marker unset and is tried again on the next start
    private static void seedOnce(DataSource dataSource, List<String> seedFiles) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (!seeded(connection)) {
                    for (String file : seedFiles) {
                        ScriptUtils.executeSqlScript(connection,
                                new EncodedResource(new FileSystemResource(file), StandardCharsets.UTF_8));
                    }
                    try (Statement statement = connection.createStatement()) {
                        statement.executeUpdate("UPDATE demo.marker SET seeded = true");
                    }
                    log.info("Loaded the demo data from {}", seedFiles);
                }
                connection.commit();
            } catch (RuntimeException | SQLException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not load the demo data", e);
        }
    }

    private static boolean seeded(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT seeded FROM demo.marker FOR UPDATE")) {
            return result.next() && result.getBoolean(1);
        }
    }
}

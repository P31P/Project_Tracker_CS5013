package tracker.projects;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** The tags people can pick from: the lab's built-in tags in groups, and custom tags already in use. */
@RestController
public class TagController {

    private static final Map<String, String> GROUPS = new LinkedHashMap<>();

    static {
        GROUPS.put("DIRECTION", "Broad direction");
        GROUPS.put("METHOD", "Methodology");
        GROUPS.put("OTHER", "Other");
    }

    /**
     * A built-in tag.
     *
     * @param name the tag as stored, lowercased
     * @param label the tag as shown, e.g. "Machine Learning"
     */
    public record BuiltInTag(String name, String label) {
    }

    /**
     * One group of built-in tags.
     *
     * @param category {@code DIRECTION}, {@code METHOD} or {@code OTHER}
     * @param label the group's heading
     * @param tags its tags in the lab's order
     */
    public record TagGroup(String category, String label, List<BuiltInTag> tags) {
    }

    /**
     * Everything the tag picker shows.
     *
     * @param groups the built-in tags in their groups
     * @param custom other tags already used on projects, ideas, papers or profiles, most used first
     */
    public record TagCatalog(List<TagGroup> groups, List<String> custom) {
    }

    // every tag in use anywhere, counted, without the built-in ones
    private static final String CUSTOM_SQL = """
            SELECT tag FROM (
                SELECT unnest(tags) AS tag FROM project
                UNION ALL SELECT unnest(tags) FROM idea
                UNION ALL SELECT unnest(tags) FROM paper
                UNION ALL SELECT unnest(interests) FROM app_user
            ) used
            WHERE tag NOT IN (SELECT name FROM tag)
            GROUP BY tag
            ORDER BY count(*) DESC, tag
            LIMIT 50
            """;

    private final JdbcClient jdbc;

    /**
     * Creates the controller.
     *
     * @param jdbc reads the tags
     */
    public TagController(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Returns the built-in tags in groups and up to 50 custom tags in use. Handles {@code GET /api/tags}.
     *
     * @return the tag catalog
     */
    @GetMapping("/api/tags")
    public TagCatalog catalog() {
        Map<String, List<BuiltInTag>> byCategory = new LinkedHashMap<>();
        GROUPS.keySet().forEach(category -> byCategory.put(category, new ArrayList<>()));
        jdbc.sql("SELECT name, label, category FROM tag ORDER BY position, name")
                .query((RowCallbackHandler) rs -> byCategory.get(rs.getString("category"))
                        .add(new BuiltInTag(rs.getString("name"), rs.getString("label"))));
        List<TagGroup> groups = GROUPS.entrySet().stream()
                .map(group -> new TagGroup(group.getKey(), group.getValue(), byCategory.get(group.getKey())))
                .toList();
        return new TagCatalog(groups, jdbc.sql(CUSTOM_SQL).query(String.class).list());
    }
}

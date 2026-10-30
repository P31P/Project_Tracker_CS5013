package tracker.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;
import tracker.support.PostgresTestBase;

@Transactional
class SearchServiceTest extends PostgresTestBase {

    @Autowired
    private SearchService search;

    @Autowired
    private JdbcClient jdbc;

    @BeforeEach
    void seed() {
        jdbc.sql("INSERT INTO project (title, description, tags) VALUES "
                + "('Flux sampling benchmark', 'Compare samplers on metabolic models', '{metabolism,benchmark}')").update();
        jdbc.sql("INSERT INTO idea (title, description) VALUES "
                + "('Sampling with Slack threads', 'Use threads as project updates')").update();
        jdbc.sql("INSERT INTO paper (normalized_url, status, title, authors, doi) VALUES "
                + "('https://doi.org/10.1000/flux', 'RESOLVED', 'Efficient flux sampling', '{Jane Doe,Ravi Raman}', "
                + "'10.1000/flux')").update();
        jdbc.sql("INSERT INTO paper (normalized_url, status) VALUES ('https://example.com/unresolved.pdf', 'PENDING')")
                .update();
    }

    @Test
    void oneKeywordFindsAProjectAnIdeaAndAPaper() {
        List<SearchResult> results = search.search("sampling", null);

        assertThat(results).extracting(SearchResult::type)
                .containsExactlyInAnyOrder(SearchType.PROJECT, SearchType.IDEA, SearchType.PAPER);
    }

    @Test
    void typeFilterKeepsOnlyThatKind() {
        List<SearchResult> results = search.search("sampling", SearchType.PAPER);

        assertThat(results).singleElement().satisfies(result -> {
            assertThat(result.type()).isEqualTo(SearchType.PAPER);
            assertThat(result.title()).isEqualTo("Efficient flux sampling");
            assertThat(result.detail()).isEqualTo("Jane Doe, Ravi Raman");
        });
    }

    @Test
    void wordsAreStemmed() {
        assertThat(search.search("sample", SearchType.PROJECT)).hasSize(1);
    }

    @Test
    void tagsAndAuthorsAreSearchable() {
        assertThat(search.search("metabolism", null)).extracting(SearchResult::type)
                .containsExactly(SearchType.PROJECT);
        assertThat(search.search("raman", null)).extracting(SearchResult::type)
                .containsExactly(SearchType.PAPER);
    }

    @Test
    void aTitleMatchOutranksADescriptionMatch() {
        jdbc.sql("INSERT INTO idea (title, description) VALUES ('Threads', 'about sampling')").update();

        List<SearchResult> results = search.search("sampling", SearchType.IDEA);

        assertThat(results).extracting(SearchResult::title)
                .containsExactly("Sampling with Slack threads", "Threads");
    }

    @Test
    void webSearchSyntaxIsUnderstood() {
        assertThat(search.search("\"flux sampling\"", null)).hasSize(2);
        assertThat(search.search("sampling -slack", null)).extracting(SearchResult::type)
                .containsExactlyInAnyOrder(SearchType.PROJECT, SearchType.PAPER);
    }

    @Test
    void blankOrUnmatchedQueriesFindNothing() {
        assertThat(search.search("  ", null)).isEmpty();
        assertThat(search.search(null, null)).isEmpty();
        assertThat(search.search("zebra", null)).isEmpty();
    }

    @Test
    void oddPunctuationDoesNotBreakTheQuery() {
        assertThat(search.search("\"unclosed (", null)).isEmpty();
    }
}

package tracker.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
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
        List<SearchResult> results = search.search("sampling", SearchFilters.NONE);

        assertThat(results).extracting(SearchResult::type)
                .containsExactlyInAnyOrder(SearchType.PROJECT, SearchType.IDEA, SearchType.PAPER);
    }

    @Test
    void typeFilterKeepsOnlyThatKind() {
        List<SearchResult> results = search.search("sampling", SearchFilters.only(SearchType.PAPER));

        assertThat(results).singleElement().satisfies(result -> {
            assertThat(result.type()).isEqualTo(SearchType.PAPER);
            assertThat(result.title()).isEqualTo("Efficient flux sampling");
            assertThat(result.detail()).isEqualTo("Jane Doe, Ravi Raman");
        });
    }

    @Test
    void wordsAreStemmed() {
        assertThat(search.search("sample", SearchFilters.only(SearchType.PROJECT))).hasSize(1);
    }

    @Test
    void tagsAndAuthorsAreSearchable() {
        assertThat(search.search("metabolism", SearchFilters.NONE)).extracting(SearchResult::type)
                .containsExactly(SearchType.PROJECT);
        assertThat(search.search("raman", SearchFilters.NONE)).extracting(SearchResult::type)
                .containsExactly(SearchType.PAPER);
    }

    @Test
    void aTitleMatchOutranksADescriptionMatch() {
        jdbc.sql("INSERT INTO idea (title, description) VALUES ('Threads', 'about sampling')").update();

        List<SearchResult> results = search.search("sampling", SearchFilters.only(SearchType.IDEA));

        assertThat(results).extracting(SearchResult::title)
                .containsExactly("Sampling with Slack threads", "Threads");
    }

    @Test
    void webSearchSyntaxIsUnderstood() {
        assertThat(search.search("\"flux sampling\"", SearchFilters.NONE)).hasSize(2);
        assertThat(search.search("sampling -slack", SearchFilters.NONE)).extracting(SearchResult::type)
                .containsExactlyInAnyOrder(SearchType.PROJECT, SearchType.PAPER);
    }

    @Test
    void blankOrUnmatchedQueriesFindNothing() {
        assertThat(search.search("  ", SearchFilters.NONE)).isEmpty();
        assertThat(search.search(null, SearchFilters.NONE)).isEmpty();
        assertThat(search.search("zebra", SearchFilters.NONE)).isEmpty();
    }

    @Test
    void oddPunctuationDoesNotBreakTheQuery() {
        assertThat(search.search("\"unclosed (", SearchFilters.NONE)).isEmpty();
    }

    @Test
    void paperIsFoundByTheIdInItsLink() {
        jdbc.sql("INSERT INTO paper (normalized_url, status, title) VALUES "
                + "('https://arxiv.org/abs/1706.03762', 'RESOLVED', 'Attention Is All You Need')").update();
        jdbc.sql("INSERT INTO paper (normalized_url, status) VALUES ('https://arxiv.org/abs/2610.13233', 'PENDING')").update();

        assertThat(search.search("1706.03762", SearchFilters.NONE)).extracting(SearchResult::title)
                .containsExactly("Attention Is All You Need");
        assertThat(search.search("2610.13233", SearchFilters.NONE)).extracting(SearchResult::title)
                .containsExactly("https://arxiv.org/abs/2610.13233");
    }

    @Test
    void tagFilterNeedsEveryTag() {
        assertThat(search.search(null, tags("metabolism"))).extracting(SearchResult::title)
                .containsExactly("Flux sampling benchmark");
        assertThat(search.search(null, tags("metabolism", "benchmark"))).hasSize(1);
        assertThat(search.search(null, tags("metabolism", "genomics"))).isEmpty();
    }

    @Test
    void paperTagsCanBeFilteredAndSearched() {
        jdbc.sql("UPDATE paper SET tags = '{genomics}' WHERE normalized_url = 'https://doi.org/10.1000/flux'").update();

        assertThat(search.search(null, tags("genomics"))).extracting(SearchResult::type)
                .containsExactly(SearchType.PAPER);
        assertThat(search.search("genomics", SearchFilters.NONE)).extracting(SearchResult::tags)
                .containsExactly(List.of("genomics"));
    }

    @Test
    void personFilterKeepsTheirProjectsAndIdeasButNoPapers() {
        long priyal = userId("cs24b047@smail.iitm.ac.in");
        long sujal = userId("da24b026@smail.iitm.ac.in");
        jdbc.sql("INSERT INTO project (title, owner_id) VALUES ('Owned sampling', :me)").param("me", priyal).update();
        long shared = jdbc.sql("INSERT INTO project (title, owner_id) VALUES ('Shared sampling', :other) RETURNING id")
                .param("other", sujal).query(Long.class).single();
        jdbc.sql("INSERT INTO project_member (project_id, user_id) VALUES (:p, :me)")
                .param("p", shared).param("me", priyal).update();
        jdbc.sql("INSERT INTO idea (title, author_id) VALUES ('My sampling idea', :me)").param("me", priyal).update();
        jdbc.sql("INSERT INTO idea (title, author_id) VALUES ('Their sampling idea', :other)")
                .param("other", sujal).update();

        SearchFilters mine = new SearchFilters(null, List.of(), priyal, null, null);

        assertThat(search.search("sampling", mine)).extracting(SearchResult::title)
                .containsExactlyInAnyOrder("Owned sampling", "Shared sampling", "My sampling idea");
    }

    @Test
    void dateRangeLeavesOutWhatWasAddedOutsideIt() {
        jdbc.sql("INSERT INTO idea (title, created_at) VALUES ('Old sampling idea', '2020-03-01 12:00+00')").update();

        SearchFilters in2020 = new SearchFilters(null, List.of(), null, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31));
        SearchFilters sinceToday = new SearchFilters(null, List.of(), null, LocalDate.now().minusDays(1), null);

        assertThat(search.search("sampling", in2020)).extracting(SearchResult::title)
                .containsExactly("Old sampling idea");
        assertThat(search.search("sampling", sinceToday)).extracting(SearchResult::title)
                .doesNotContain("Old sampling idea");
    }

    @Test
    void filtersWithoutAQueryListNewestFirst() {
        jdbc.sql("INSERT INTO idea (title, tags, created_at) VALUES ('Older', '{graphs}', now() - interval '2 days')").update();
        jdbc.sql("INSERT INTO idea (title, tags) VALUES ('Newer', '{graphs}')").update();

        assertThat(search.search("  ", tags("graphs"))).extracting(SearchResult::title)
                .containsExactly("Newer", "Older");
    }

    @Test
    void aTypeAloneWithoutAQueryFindsNothing() {
        assertThat(search.search(null, SearchFilters.only(SearchType.PROJECT))).isEmpty();
    }

    @Test
    void theWriteUpOfProjectsAndIdeasIsSearchable() {
        jdbc.sql("INSERT INTO project (title, core_question, challenges) VALUES "
                + "('Plain title', 'Can we predict growth rates?', 'Noisy chemostat data')").update();
        jdbc.sql("INSERT INTO idea (title, approach) VALUES ('Another plain title', 'Use a chemostat')").update();

        assertThat(search.search("growth rates", SearchFilters.NONE)).extracting(SearchResult::title)
                .containsExactly("Plain title");
        assertThat(search.search("chemostat", SearchFilters.NONE)).extracting(SearchResult::title)
                .containsExactlyInAnyOrder("Plain title", "Another plain title");
    }

    private static SearchFilters tags(String... tags) {
        return new SearchFilters(null, List.of(tags), null, null, null);
    }

    private long userId(String email) {
        return jdbc.sql("SELECT id FROM app_user WHERE email = :email").param("email", email).query(Long.class).single();
    }
}

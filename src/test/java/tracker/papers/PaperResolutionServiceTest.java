package tracker.papers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import tracker.data.Paper;
import tracker.data.PaperRepository;
import tracker.data.PaperSource;
import tracker.data.PaperStatus;
import tracker.papers.resolver.PaperResolver;
import tracker.papers.resolver.ResolvedMetadata;
import tracker.support.PostgresTestBase;

@Transactional
class PaperResolutionServiceTest extends PostgresTestBase {

    private static final String URL = "https://arxiv.org/abs/1706.03762";

    @Autowired
    private PaperRepository papers;

    @Autowired
    private EntityManager entityManager;

    private PaperResolver resolver;
    private PaperResolutionService service;

    @BeforeEach
    void setUp() {
        resolver = mock(PaperResolver.class);
        when(resolver.supports(any())).thenReturn(true);
        service = new PaperResolutionService(papers, List.of(resolver));
    }

    @Test
    void foundPaperIsResolvedWithItsDetails() {
        Paper paper = papers.saveAndFlush(new Paper(URL));
        when(resolver.resolve(URL)).thenReturn(Optional.of(new ResolvedMetadata(
                "Attention Is All You Need", List.of("Ashish Vaswani", "Noam Shazeer"),
                LocalDate.of(2017, 6, 12), null, PaperSource.REGISTRY)));

        assertThat(service.resolveNext()).isTrue();

        Paper saved = reload(paper);
        assertThat(saved.getStatus()).isEqualTo(PaperStatus.RESOLVED);
        assertThat(saved.getSource()).isEqualTo(PaperSource.REGISTRY);
        assertThat(saved.getTitle()).isEqualTo("Attention Is All You Need");
        assertThat(saved.getAuthors()).containsExactly("Ashish Vaswani", "Noam Shazeer");
        assertThat(saved.getPublishedOn()).isEqualTo(LocalDate.of(2017, 6, 12));
    }

    @Test
    void unknownPaperIsMarkedUnresolved() {
        Paper paper = papers.saveAndFlush(new Paper(URL));
        when(resolver.resolve(URL)).thenReturn(Optional.empty());

        service.resolveNext();

        assertThat(reload(paper).getStatus()).isEqualTo(PaperStatus.UNRESOLVED);
    }

    @Test
    void linkNoResolverSupportsIsMarkedUnresolved() {
        Paper paper = papers.saveAndFlush(new Paper("https://example.org/paper.pdf"));
        when(resolver.supports("https://example.org/paper.pdf")).thenReturn(false);

        service.resolveNext();

        assertThat(reload(paper).getStatus()).isEqualTo(PaperStatus.UNRESOLVED);
    }

    @Test
    void failureKeepsPaperPendingUntilTheThirdAttempt() {
        Paper paper = papers.saveAndFlush(new Paper(URL));
        when(resolver.resolve(URL)).thenThrow(new IllegalStateException("503"));

        service.resolveNext();
        retryDelayPasses();
        service.resolveNext();
        assertThat(reload(paper).getStatus()).isEqualTo(PaperStatus.PENDING);
        assertThat(reload(paper).getAttempts()).isEqualTo(2);

        retryDelayPasses();
        service.resolveNext();
        assertThat(reload(paper).getStatus()).isEqualTo(PaperStatus.UNRESOLVED);
    }

    @Test
    void failedPaperWaitsBeforeItIsTriedAgain() {
        papers.saveAndFlush(new Paper(URL));
        when(resolver.resolve(URL)).thenThrow(new IllegalStateException("503"));

        service.resolveNext();

        assertThat(service.resolveNext()).isFalse();
    }

    @Test
    void oneWorkerRunDoesNotUseUpAllAttempts() {
        Paper paper = papers.saveAndFlush(new Paper(URL));
        when(resolver.resolve(URL)).thenThrow(new IllegalStateException("503"));

        new PaperResolutionWorker(service).resolvePending();

        Paper saved = reload(paper);
        assertThat(saved.getStatus()).isEqualTo(PaperStatus.PENDING);
        assertThat(saved.getAttempts()).isEqualTo(1);
    }

    @Test
    void failingPaperDoesNotBlockNewerOnes() {
        Paper failing = papers.saveAndFlush(new Paper("https://arxiv.org/abs/1111.11111"));
        Paper newer = papers.saveAndFlush(new Paper(URL));
        when(resolver.resolve(failing.getNormalizedUrl())).thenThrow(new IllegalStateException("503"));
        when(resolver.resolve(URL)).thenReturn(Optional.of(new ResolvedMetadata("Title", List.of(), null, null, PaperSource.REGISTRY)));

        service.resolveNext();
        service.resolveNext();

        assertThat(reload(newer).getStatus()).isEqualTo(PaperStatus.RESOLVED);
    }

    @Test
    void returnsFalseWhenNothingIsPending() {
        Paper paper = new Paper(URL);
        paper.setStatus(PaperStatus.RESOLVED);
        papers.saveAndFlush(paper);

        assertThat(service.resolveNext()).isFalse();
    }

    private void retryDelayPasses() {
        papers.flush();
        entityManager.createNativeQuery("UPDATE paper SET next_attempt_at = NULL").executeUpdate();
        entityManager.clear();
    }

    private Paper reload(Paper paper) {
        papers.flush();
        entityManager.clear(); // read back from the database, not the cached entity
        return papers.findById(paper.getId()).orElseThrow();
    }
}

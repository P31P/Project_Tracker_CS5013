package tracker.papers;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tracker.data.Paper;
import tracker.data.PaperReference;
import tracker.data.PaperReferenceRepository;
import tracker.data.PaperRepository;
import tracker.papers.resolver.PaperResolver;
import tracker.papers.resolver.ResolvedMetadata;
import tracker.papers.resolver.ResolvedReference;

/** Looks up the details of pending papers, one paper per transaction, and records the outcome. */
@Service
public class PaperResolutionService {

    /** Failed lookups allowed before a paper is marked unresolved. */
    static final int MAX_ATTEMPTS = 3;
    /** The wait after the first failed lookup; later failures wait longer. */
    static final Duration RETRY_DELAY = Duration.ofMinutes(10);

    private static final Logger log = LoggerFactory.getLogger(PaperResolutionService.class);

    private final PaperRepository papers;
    private final PaperReferenceRepository references;
    private final List<PaperResolver> resolvers;

    /**
     * Creates the service.
     *
     * @param papers where papers are stored
     * @param references where papers' reference lists are stored
     * @param resolvers the resolvers, in the order to try them
     */
    public PaperResolutionService(PaperRepository papers, PaperReferenceRepository references,
                                  List<PaperResolver> resolvers) {
        this.papers = papers;
        this.references = references;
        this.resolvers = resolvers;
    }

    /**
     * Resolves the next pending paper that is due, in its own transaction. A paper no resolver knows becomes
     * unresolved; a failed lookup keeps it pending and retries later, up to three times.
     *
     * @return {@code false} if no paper was due
     */
    @Transactional
    public boolean resolveNext() {
        Optional<Paper> next = papers.claimNextPending();
        if (next.isEmpty()) {
            return false;
        }
        resolve(next.get());
        return true;
    }

    private void resolve(Paper paper) {
        try {
            Optional<ResolvedMetadata> found = lookup(paper.getNormalizedUrl());
            if (found.isPresent()) {
                ResolvedMetadata meta = found.get();
                paper.markResolved(meta.title(), meta.authors(), meta.publishedOn(), meta.doi(), meta.source());
                replaceReferences(paper, meta.references());
            } else {
                paper.markUnresolved();
            }
        } catch (RuntimeException e) {
            // network or server trouble: stay pending and try again later, but not forever
            log.warn("Could not resolve {}: {}", paper.getNormalizedUrl(), e.toString());
            paper.recordFailedAttempt(RETRY_DELAY);
            if (paper.getAttempts() >= MAX_ATTEMPTS) {
                paper.markUnresolved();
            }
        }
    }

    private void replaceReferences(Paper paper, List<ResolvedReference> found) {
        references.deleteByPaperId(paper.getId());
        for (int i = 0; i < found.size(); i++) {
            ResolvedReference reference = found.get(i);
            references.save(new PaperReference(paper, i + 1, reference.citation(), reference.doi()));
        }
    }

    private Optional<ResolvedMetadata> lookup(String normalizedUrl) {
        for (PaperResolver resolver : resolvers) {
            if (resolver.supports(normalizedUrl)) {
                Optional<ResolvedMetadata> found = resolver.resolve(normalizedUrl);
                if (found.isPresent()) {
                    return found;
                }
            }
        }
        return Optional.empty();
    }
}

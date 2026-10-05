package tracker.papers;

import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tracker.data.Paper;
import tracker.data.PaperRepository;
import tracker.data.PaperSource;
import tracker.papers.resolver.PaperResolver;
import tracker.papers.resolver.ResolvedMetadata;

@Service
public class PaperResolutionService {

    static final int MAX_ATTEMPTS = 3;

    private static final Logger log = LoggerFactory.getLogger(PaperResolutionService.class);

    private final PaperRepository papers;
    private final List<PaperResolver> resolvers;

    public PaperResolutionService(PaperRepository papers, List<PaperResolver> resolvers) {
        this.papers = papers;
        this.resolvers = resolvers;
    }

    /** Resolves one pending paper in its own transaction. Returns false when nothing was pending. */
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
                paper.markResolved(meta.title(), meta.authors(), meta.publishedOn(), meta.doi(),
                        PaperSource.REGISTRY);
            } else {
                paper.markUnresolved();
            }
        } catch (RuntimeException e) {
            // network or server trouble: stay pending and try again later, but not forever
            log.warn("Could not resolve {}: {}", paper.getNormalizedUrl(), e.toString());
            paper.recordFailedAttempt();
            if (paper.getAttempts() >= MAX_ATTEMPTS) {
                paper.markUnresolved();
            }
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

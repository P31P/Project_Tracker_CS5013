package tracker.papers.resolver;

import java.util.Optional;

public interface PaperResolver {

    boolean supports(String normalizedUrl);

    /** Empty if the source does not know the paper. Network failures are thrown so the caller can retry. */
    Optional<ResolvedMetadata> resolve(String normalizedUrl);
}

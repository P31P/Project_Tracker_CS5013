package tracker.papers.resolver;

import java.util.Optional;

/**
 * One way of finding a paper's details from its normalized address. Resolvers are tried in {@code @Order}:
 * Crossref, arXiv, then the page's meta tags.
 */
public interface PaperResolver {

    /**
     * Says whether this resolver can look up a kind of address.
     *
     * @param normalizedUrl the paper's normalized address
     * @return {@code true} if {@link #resolve} should be tried for it
     */
    boolean supports(String normalizedUrl);

    /**
     * Looks up a paper's details.
     *
     * @param normalizedUrl the paper's normalized address
     * @return the details, or empty if the source does not know the paper
     * @throws RuntimeException on network or server failures, so the caller can retry later
     */
    Optional<ResolvedMetadata> resolve(String normalizedUrl);
}

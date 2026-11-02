package tracker.papers.resolver;

/**
 * One entry in a paper's reference list, as a registry gives it.
 *
 * @param citation the reference as text, e.g. "Ahonen, T. et al. (2006). Face description with ..."
 * @param doi the cited work's DOI in lowercase, or {@code null} if the registry doesn't know it
 */
public record ResolvedReference(String citation, String doi) {
}

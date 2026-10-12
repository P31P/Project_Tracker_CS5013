package tracker.data;

/**
 * Where a paper's details came from.
 */
public enum PaperSource {
    /** A registry such as Crossref or arXiv. */
    REGISTRY,
    /** The citation meta tags on the paper's own web page. */
    META_TAGS
}

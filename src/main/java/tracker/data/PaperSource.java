package tracker.data;

/**
 * Where a paper's details came from.
 */
public enum PaperSource {
    /** A registry such as Crossref or arXiv. */
    REGISTRY,
    /** The citation meta tags on the paper's own web page. */
    META_TAGS,
    /** A person corrected the details by hand. */
    MANUAL,
    /** The language model read them from the PDF, so a person should check them. */
    MODEL
}

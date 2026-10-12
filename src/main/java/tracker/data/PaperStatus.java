package tracker.data;

/** How far the app got in finding a paper's details. */
public enum PaperStatus {
    /** Waiting for the worker to look it up, possibly after a failed attempt. */
    PENDING,
    /** Its details were found. */
    RESOLVED,
    /** No source knew it, or every attempt failed. */
    UNRESOLVED
}

package tracker.data;

/**
 * What a timeline entry ({@link ProjectUpdate}) is: a note someone wrote or a record of a status change.
 */
public enum UpdateKind {
    /** Something a person wrote. */
    NOTE,
    /** Written by the app when the project's status changed. */
    STATUS
}

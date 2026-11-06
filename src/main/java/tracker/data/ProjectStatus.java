package tracker.data;

/** Where a project stands. Each status has a label for showing to people. */
public enum ProjectStatus {
    /** Being planned; work has not started. */
    PLANNING("Planning"),
    /** Being worked on. New projects start here unless told otherwise. */
    ACTIVE("Ongoing"),
    /** Paused for now. */
    ON_HOLD("On hold"),
    /** Finished. */
    DONE("Completed");

    private final String label;

    ProjectStatus(String label) {
        this.label = label;
    }

    /**
     * Returns the status as people read it, e.g. "On hold".
     *
     * @return the label for this status
     */
    public String label() {
        return label;
    }
}

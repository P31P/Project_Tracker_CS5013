package tracker.data;

public enum ProjectStatus {
    PLANNING("Planning"),
    ACTIVE("Active"),
    ON_HOLD("On hold"),
    DONE("Done");

    private final String label;

    ProjectStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}

package tracker.data;

/** What someone may do in the tracker, stored in {@code app_user.role}. */
public enum Role {
    /** A lab member. */
    MEMBER("Member"),
    /** A lab member who can also give and remove access. */
    LAB_MANAGER("Lab Manager"),
    /** The lab's head: manages access too, and sees the lab-wide numbers on Home. */
    ADMIN("Admin");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    /**
     * Returns the role as people read it.
     *
     * @return the role's name, e.g. "Lab Manager"
     */
    public String label() {
        return label;
    }

    /**
     * Says whether this role may add people, remove their access and change roles.
     *
     * @return {@code true} for a Lab Manager or an admin
     */
    public boolean managesAccess() {
        return this != MEMBER;
    }
}

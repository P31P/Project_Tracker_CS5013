package tracker.papers;

/**
 * A project or an idea that lists a paper as a reference.
 *
 * @param type {@code PROJECT} or {@code IDEA}
 * @param id the project's or idea's id
 * @param title its title
 */
public record PaperUse(String type, long id, String title) {
}

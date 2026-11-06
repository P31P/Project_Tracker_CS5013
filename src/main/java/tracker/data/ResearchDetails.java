package tracker.data;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * The longer write-up of a project or an idea: the question, why it matters, how to go about it, what it needs and
 * what could go wrong. Every part is optional Markdown. Projects and ideas store it in the same five columns.
 */
@Embeddable
public class ResearchDetails {

    @Column(name = "core_question")
    private String coreQuestion;

    private String motivation;

    private String approach;

    private String resources;

    private String challenges;

    /** Creates empty details. */
    public ResearchDetails() {
    }

    /**
     * Creates details from their five parts.
     *
     * @param coreQuestion the question it tries to answer; may be {@code null}
     * @param motivation why it matters; may be {@code null}
     * @param approach how it could be done; may be {@code null}
     * @param resources the data and resources it needs; may be {@code null}
     * @param challenges what could make it hard; may be {@code null}
     */
    public ResearchDetails(String coreQuestion, String motivation, String approach, String resources,
                           String challenges) {
        this.coreQuestion = coreQuestion;
        this.motivation = motivation;
        this.approach = approach;
        this.resources = resources;
        this.challenges = challenges;
    }

    /**
     * Returns the question it tries to answer, or {@code null}.
     *
     * @return the core question, or {@code null}
     */
    public String getCoreQuestion() {
        return coreQuestion;
    }

    /**
     * Returns why it matters, or {@code null}.
     *
     * @return the motivation, or {@code null}
     */
    public String getMotivation() {
        return motivation;
    }

    /**
     * Returns how it could be done, or {@code null}.
     *
     * @return the proposed approach, or {@code null}
     */
    public String getApproach() {
        return approach;
    }

    /**
     * Returns the data and resources it needs, or {@code null}.
     *
     * @return the data and resources, or {@code null}
     */
    public String getResources() {
        return resources;
    }

    /**
     * Returns what could make it hard, or {@code null}.
     *
     * @return the challenges, or {@code null}
     */
    public String getChallenges() {
        return challenges;
    }
}

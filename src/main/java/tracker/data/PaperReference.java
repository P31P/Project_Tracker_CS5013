package tracker.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** One entry in a {@link Paper}'s reference list, kept in the order the registry gave. */
@Entity
@Table(name = "paper_reference")
public class PaperReference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paper_id")
    private Paper paper;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private String citation;

    private String doi;

    /** Used by JPA. */
    protected PaperReference() {
    }

    /**
     * Creates a reference.
     *
     * @param paper the paper whose reference list it is in
     * @param position its place in the list, starting at 1
     * @param citation the reference as text; must not be blank
     * @param doi the cited work's DOI, or {@code null}
     */
    public PaperReference(Paper paper, int position, String citation, String doi) {
        this.paper = paper;
        this.position = position;
        this.citation = citation;
        this.doi = doi;
    }

    /**
     * Returns the reference's place in the list, starting at 1.
     *
     * @return the reference's place in the list, starting at 1
     */
    public int getPosition() {
        return position;
    }

    /**
     * Returns the reference as text.
     *
     * @return the reference as text
     */
    public String getCitation() {
        return citation;
    }

    /**
     * Returns the cited work's DOI, or {@code null} if unknown.
     *
     * @return the cited work's DOI, or {@code null} if unknown
     */
    public String getDoi() {
        return doi;
    }
}

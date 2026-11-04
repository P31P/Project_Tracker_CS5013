package tracker.papers;

import tracker.data.PaperReference;

/**
 * One entry in a paper's reference list, as returned by the papers API.
 *
 * @param position its place in the list, starting at 1
 * @param citation the reference as text
 * @param doi the cited work's DOI, or {@code null}
 */
public record PaperReferenceDto(int position, String citation, String doi) {

    /**
     * Builds the view of a reference.
     *
     * @param reference the stored reference
     * @return the reference as the API returns it
     */
    static PaperReferenceDto from(PaperReference reference) {
        return new PaperReferenceDto(reference.getPosition(), reference.getCitation(), reference.getDoi());
    }
}

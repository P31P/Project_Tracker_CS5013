package tracker.papers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tracker.data.Paper;
import tracker.data.PaperRepository;
import tracker.data.PaperShare;
import tracker.data.PaperShareRepository;
import tracker.data.PaperSource;
import tracker.data.PaperStatus;

/** The papers API under {@code /api/papers}. */
@RestController
@RequestMapping("/api/papers")
public class PaperController {

    private final PaperRepository papers;
    private final PaperShareRepository shares;

    /**
     * Creates the controller.
     *
     * @param papers where papers are stored
     * @param shares where Slack shares are stored
     */
    public PaperController(PaperRepository papers, PaperShareRepository shares) {
        this.papers = papers;
        this.shares = shares;
    }

    /**
     * Lists papers. Handles {@code GET /api/papers}.
     *
     * @param status only papers with this status; all papers if left out
     * @return the papers
     */
    @GetMapping
    public List<PaperDto> list(@RequestParam(required = false) PaperStatus status) {
        return toDtos(status != null ? papers.findByStatus(status) : papers.findAll());
    }

    /**
     * Returns one paper. Handles {@code GET /api/papers/{id}}.
     *
     * @param id the paper's id
     * @return the paper
     * @throws ResponseStatusException {@code 404} if there is no paper with this id
     */
    @GetMapping("/{id}")
    public PaperDto get(@PathVariable Long id) {
        Paper paper = papers.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return toDtos(List.of(paper)).get(0);
    }

    /**
     * Replaces a paper's details with a person's corrections and marks them as entered by hand. Handles
     * {@code PATCH /api/papers/{id}}. A paper still waiting for lookup is resolved too, so the worker skips it.
     *
     * @param id the paper's id
     * @param edit the corrected details
     * @return the updated paper
     * @throws ResponseStatusException {@code 404} if there is no paper with this id, {@code 400} if the title is blank
     */
    @PatchMapping("/{id}")
    public PaperDto fix(@PathVariable Long id, @RequestBody PaperEdit edit) {
        Paper paper = papers.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No such paper"));
        String title = edit.title() == null ? "" : edit.title().strip();
        if (title.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A paper needs a title");
        }
        List<String> authors = edit.authors() == null ? List.of()
                : edit.authors().stream().filter(name -> name != null && !name.isBlank()).map(String::strip).toList();
        String doi = edit.doi() == null || edit.doi().isBlank() ? null : edit.doi().strip();
        paper.markResolved(title, authors, edit.publishedOn(), doi, PaperSource.MANUAL);
        papers.save(paper);
        return toDtos(List.of(paper)).get(0);
    }

    private List<PaperDto> toDtos(List<Paper> found) {
        List<Long> ids = found.stream().map(Paper::getId).toList();
        Map<Long, PaperShare> firstShare = new HashMap<>();
        for (PaperShare share : shares.findByPaperIds(ids)) {
            firstShare.putIfAbsent(share.getPaper().getId(), share); // shares come oldest first
        }
        return found.stream().map(paper -> PaperDto.from(paper, firstShare.get(paper.getId()))).toList();
    }
}

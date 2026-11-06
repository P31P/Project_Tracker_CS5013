package tracker.papers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tracker.auth.CurrentUser;
import tracker.data.AppUser;
import tracker.data.Paper;
import tracker.data.PaperReferenceRepository;
import tracker.data.PaperRepository;
import tracker.data.PaperShare;
import tracker.data.PaperShareRepository;
import tracker.data.PaperSource;
import tracker.data.PaperStatus;
import tracker.projects.ProjectService;

/** The papers API under {@code /api/papers}. */
@RestController
@RequestMapping("/api/papers")
public class PaperController {

    private final PaperRepository papers;
    private final PaperShareRepository shares;
    private final PaperReferenceRepository references;
    private final PaperMarks marks;
    private final CurrentUser currentUser;
    private final JdbcClient jdbc;

    /**
     * Creates the controller.
     *
     * @param papers where papers are stored
     * @param shares where Slack shares are stored
     * @param references where papers' reference lists are stored
     * @param marks where upvotes and read marks are stored
     * @param currentUser finds the signed-in user
     * @param jdbc finds the projects and ideas that list a paper
     */
    public PaperController(PaperRepository papers, PaperShareRepository shares, PaperReferenceRepository references,
                           PaperMarks marks, CurrentUser currentUser, JdbcClient jdbc) {
        this.papers = papers;
        this.shares = shares;
        this.references = references;
        this.marks = marks;
        this.currentUser = currentUser;
        this.jdbc = jdbc;
    }

    /**
     * Lists papers. Handles {@code GET /api/papers}.
     *
     * @param status only papers with this status; all papers if left out
     * @param user the signed-in user, to mark their votes and read papers
     * @return the papers
     */
    @GetMapping
    public List<PaperDto> list(@RequestParam(required = false) PaperStatus status,
                               @AuthenticationPrincipal OidcUser user) {
        return toDtos(status != null ? papers.findByStatus(status) : papers.findAll(), currentUser.of(user));
    }

    /**
     * Returns one paper. Handles {@code GET /api/papers/{id}}.
     *
     * @param id the paper's id
     * @param user the signed-in user, to mark their vote and read mark
     * @return the paper
     * @throws ResponseStatusException {@code 404} if there is no paper with this id
     */
    @GetMapping("/{id}")
    public PaperDto get(@PathVariable Long id, @AuthenticationPrincipal OidcUser user) {
        return view(find(id), currentUser.of(user));
    }

    /**
     * Lists a paper's references in their original order. Handles {@code GET /api/papers/{id}/references}. Only
     * papers found in Crossref have them; for others the list is empty.
     *
     * @param id the paper's id
     * @return the references
     * @throws ResponseStatusException {@code 404} if there is no paper with this id
     */
    @GetMapping("/{id}/references")
    public List<PaperReferenceDto> references(@PathVariable Long id) {
        if (!papers.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No such paper");
        }
        return references.findByPaperIdOrderByPosition(id).stream().map(PaperReferenceDto::from).toList();
    }

    /**
     * Lists the projects and ideas that have the paper among their references, projects first, by title. Handles
     * {@code GET /api/papers/{id}/used-in}.
     *
     * @param id the paper's id
     * @return where the paper is used
     * @throws ResponseStatusException {@code 404} if there is no paper with this id
     */
    @GetMapping("/{id}/used-in")
    public List<PaperUse> usedIn(@PathVariable Long id) {
        find(id);
        return jdbc.sql("""
                        SELECT 'PROJECT' AS type, p.id, p.title FROM project_paper pp JOIN project p ON p.id = pp.project_id
                        WHERE pp.paper_id = :paper
                        UNION ALL
                        SELECT 'IDEA', i.id, i.title FROM idea_paper ip JOIN idea i ON i.id = ip.idea_id
                        WHERE ip.paper_id = :paper
                        ORDER BY type DESC, title
                        """)
                .param("paper", id)
                .query((rs, row) -> new PaperUse(rs.getString("type"), rs.getLong("id"), rs.getString("title")))
                .list();
    }

    /**
     * Replaces a paper's details with a person's corrections and marks them as entered by hand. Handles
     * {@code PATCH /api/papers/{id}}. A paper still waiting for lookup is resolved too, so the worker skips it.
     *
     * @param id the paper's id
     * @param edit the corrected details
     * @param user the signed-in user
     * @return the updated paper
     * @throws ResponseStatusException {@code 404} if there is no paper with this id, {@code 400} if the title is blank
     */
    @PatchMapping("/{id}")
    public PaperDto fix(@PathVariable Long id, @RequestBody PaperEdit edit, @AuthenticationPrincipal OidcUser user) {
        Paper paper = find(id);
        String title = edit.title() == null ? "" : edit.title().strip();
        if (title.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A paper needs a title");
        }
        List<String> authors = edit.authors() == null ? List.of()
                : edit.authors().stream().filter(name -> name != null && !name.isBlank()).map(String::strip).toList();
        String doi = edit.doi() == null || edit.doi().isBlank() ? null : edit.doi().strip();
        paper.markResolved(title, authors, edit.publishedOn(), doi, PaperSource.MANUAL);
        papers.save(paper);
        return view(paper, currentUser.of(user));
    }

    /**
     * Replaces a paper's tags. Handles {@code PUT /api/papers/{id}/tags}. Unlike fixing the details, this
     * leaves the paper's status and source alone.
     *
     * @param id the paper's id
     * @param request the new tags
     * @param user the signed-in user
     * @return the updated paper
     * @throws ResponseStatusException {@code 404} if there is no paper with this id
     */
    @PutMapping("/{id}/tags")
    public PaperDto tag(@PathVariable Long id, @RequestBody PaperTags request, @AuthenticationPrincipal OidcUser user) {
        Paper paper = find(id);
        paper.setTags(ProjectService.tags(request.tags()));
        papers.save(paper);
        return view(paper, currentUser.of(user));
    }

    /**
     * Upvotes a paper; voting twice counts once. Handles {@code PUT /api/papers/{id}/vote}.
     *
     * @param id the paper's id
     * @param user the signed-in user
     * @return the paper with its new vote count
     * @throws ResponseStatusException {@code 404} if there is no paper with this id
     */
    @PutMapping("/{id}/vote")
    public PaperDto vote(@PathVariable Long id, @AuthenticationPrincipal OidcUser user) {
        Paper paper = find(id);
        AppUser voter = currentUser.of(user);
        marks.vote(id, voter);
        return view(paper, voter);
    }

    /**
     * Takes back an upvote. Handles {@code DELETE /api/papers/{id}/vote}.
     *
     * @param id the paper's id
     * @param user the signed-in user
     * @return the paper with its new vote count
     * @throws ResponseStatusException {@code 404} if there is no paper with this id
     */
    @DeleteMapping("/{id}/vote")
    public PaperDto unvote(@PathVariable Long id, @AuthenticationPrincipal OidcUser user) {
        Paper paper = find(id);
        AppUser voter = currentUser.of(user);
        marks.unvote(id, voter);
        return view(paper, voter);
    }

    /**
     * Marks a paper as read by the signed-in user. Handles {@code PUT /api/papers/{id}/read}.
     *
     * @param id the paper's id
     * @param user the signed-in user
     * @return the paper, now marked as read
     * @throws ResponseStatusException {@code 404} if there is no paper with this id
     */
    @PutMapping("/{id}/read")
    public PaperDto markRead(@PathVariable Long id, @AuthenticationPrincipal OidcUser user) {
        Paper paper = find(id);
        AppUser reader = currentUser.of(user);
        marks.markRead(id, reader);
        return view(paper, reader);
    }

    /**
     * Marks a paper as not read yet by the signed-in user. Handles {@code DELETE /api/papers/{id}/read}.
     *
     * @param id the paper's id
     * @param user the signed-in user
     * @return the paper, no longer marked as read
     * @throws ResponseStatusException {@code 404} if there is no paper with this id
     */
    @DeleteMapping("/{id}/read")
    public PaperDto markUnread(@PathVariable Long id, @AuthenticationPrincipal OidcUser user) {
        Paper paper = find(id);
        AppUser reader = currentUser.of(user);
        marks.markUnread(id, reader);
        return view(paper, reader);
    }

    private Paper find(Long id) {
        return papers.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No such paper"));
    }

    private PaperDto view(Paper paper, AppUser viewer) {
        return toDtos(List.of(paper), viewer).get(0);
    }

    private List<PaperDto> toDtos(List<Paper> found, AppUser viewer) {
        List<Long> ids = found.stream().map(Paper::getId).toList();
        Map<Long, PaperShare> firstShare = new HashMap<>();
        for (PaperShare share : shares.findByPaperIds(ids)) {
            firstShare.putIfAbsent(share.getPaper().getId(), share); // shares come oldest first
        }
        PaperMarks.Snapshot snapshot = marks.snapshot(viewer);
        return found.stream()
                .map(paper -> PaperDto.from(paper, firstShare.get(paper.getId()), snapshot.of(paper.getId())))
                .toList();
    }
}

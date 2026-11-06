package tracker.projects;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tracker.auth.CurrentUser;
import tracker.projects.ProjectDtos.CommentRequest;
import tracker.projects.ProjectDtos.CommentView;
import tracker.projects.ProjectDtos.LinkRequest;
import tracker.projects.ProjectDtos.PaperRequest;
import tracker.projects.ProjectDtos.ProjectDetail;
import tracker.projects.ProjectDtos.ProjectRequest;
import tracker.projects.ProjectDtos.ProjectSummary;
import tracker.projects.ProjectDtos.TimelineEntry;
import tracker.projects.ProjectDtos.UpdateRequest;

/** The projects API under {@code /api/projects}. Errors come back as {@code {"error": "..."}}. */
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projects;
    private final CurrentUser currentUser;

    /**
     * Creates the controller.
     *
     * @param projects does the work
     * @param currentUser finds the signed-in user
     */
    public ProjectController(ProjectService projects, CurrentUser currentUser) {
        this.projects = projects;
        this.currentUser = currentUser;
    }

    /**
     * Lists every project, most recently changed first. Handles {@code GET /api/projects}.
     *
     * @return all projects
     */
    @GetMapping
    public List<ProjectSummary> list() {
        return projects.list();
    }

    /**
     * Creates a project. Handles {@code POST /api/projects} and answers {@code 201}.
     *
     * @param request the project; only the title is required
     * @param user the signed-in user, who becomes the owner
     * @return the new project
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectDetail create(@RequestBody ProjectRequest request, @AuthenticationPrincipal OidcUser user) {
        return projects.create(request, currentUser.of(user));
    }

    /**
     * Returns one project with its timeline and links. Handles {@code GET /api/projects/{id}}.
     *
     * @param id the project's id
     * @param user the signed-in user, to mark their vote
     * @return the project
     */
    @GetMapping("/{id}")
    public ProjectDetail get(@PathVariable Long id, @AuthenticationPrincipal OidcUser user) {
        return projects.get(id, currentUser.of(user));
    }

    /**
     * Changes a project; fields left out are kept. Handles {@code PATCH /api/projects/{id}}.
     *
     * @param id the project's id
     * @param request the changes
     * @param user the signed-in user, named on a status change
     * @return the changed project
     */
    @PatchMapping("/{id}")
    public ProjectDetail update(@PathVariable Long id, @RequestBody ProjectRequest request,
                                @AuthenticationPrincipal OidcUser user) {
        return projects.update(id, request, currentUser.of(user));
    }

    /**
     * Adds a note to a project's timeline. Handles {@code POST /api/projects/{id}/updates} and answers {@code 201}.
     *
     * @param id the project's id
     * @param request the note
     * @param user the signed-in user, who becomes the author
     * @return the new entry
     */
    @PostMapping("/{id}/updates")
    @ResponseStatus(HttpStatus.CREATED)
    public TimelineEntry addUpdate(@PathVariable Long id, @RequestBody UpdateRequest request,
                                   @AuthenticationPrincipal OidcUser user) {
        return projects.addUpdate(id, request.body(), currentUser.of(user));
    }

    /**
     * Adds a link to a project. Handles {@code POST /api/projects/{id}/links} and answers {@code 201}.
     *
     * @param id the project's id
     * @param request the link
     * @param user the signed-in user
     * @return the project with the new link
     */
    @PostMapping("/{id}/links")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectDetail addLink(@PathVariable Long id, @RequestBody LinkRequest request,
                                 @AuthenticationPrincipal OidcUser user) {
        return projects.addLink(id, request, currentUser.of(user));
    }

    /**
     * Removes a link from a project. Handles {@code DELETE /api/projects/{id}/links/{linkId}}.
     *
     * @param id the project's id
     * @param linkId the link's id
     * @param user the signed-in user
     * @return the project without the link
     */
    @DeleteMapping("/{id}/links/{linkId}")
    public ProjectDetail removeLink(@PathVariable Long id, @PathVariable Long linkId,
                                    @AuthenticationPrincipal OidcUser user) {
        return projects.removeLink(id, linkId, currentUser.of(user));
    }

    /**
     * Lists a paper from the archive as a reference. Handles {@code POST /api/projects/{id}/papers} and answers
     * {@code 201}.
     *
     * @param id the project's id
     * @param request the paper
     * @param user the signed-in user
     * @return the project with the paper
     */
    @PostMapping("/{id}/papers")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectDetail addPaper(@PathVariable Long id, @RequestBody PaperRequest request,
                                  @AuthenticationPrincipal OidcUser user) {
        return projects.addPaper(id, request.paperId(), currentUser.of(user));
    }

    /**
     * Takes a paper off the references. Handles {@code DELETE /api/projects/{id}/papers/{paperId}}.
     *
     * @param id the project's id
     * @param paperId the paper's id
     * @param user the signed-in user
     * @return the project without the paper
     */
    @DeleteMapping("/{id}/papers/{paperId}")
    public ProjectDetail removePaper(@PathVariable Long id, @PathVariable Long paperId,
                                     @AuthenticationPrincipal OidcUser user) {
        return projects.removePaper(id, paperId, currentUser.of(user));
    }

    /**
     * Upvotes a project; voting twice counts once. Handles {@code PUT /api/projects/{id}/vote}.
     *
     * @param id the project's id
     * @param user the signed-in user
     * @return the project with its new vote count
     */
    @PutMapping("/{id}/vote")
    public ProjectDetail vote(@PathVariable Long id, @AuthenticationPrincipal OidcUser user) {
        return projects.vote(id, currentUser.of(user));
    }

    /**
     * Takes back a vote. Handles {@code DELETE /api/projects/{id}/vote}.
     *
     * @param id the project's id
     * @param user the signed-in user
     * @return the project with its new vote count
     */
    @DeleteMapping("/{id}/vote")
    public ProjectDetail unvote(@PathVariable Long id, @AuthenticationPrincipal OidcUser user) {
        return projects.unvote(id, currentUser.of(user));
    }

    /**
     * Lists a project's discussion, oldest first. Handles {@code GET /api/projects/{id}/comments}.
     *
     * @param id the project's id
     * @return the comments
     */
    @GetMapping("/{id}/comments")
    public List<CommentView> comments(@PathVariable Long id) {
        return projects.comments(id);
    }

    /**
     * Adds a comment to a project's discussion. Handles {@code POST /api/projects/{id}/comments} and answers
     * {@code 201}.
     *
     * @param id the project's id
     * @param request the comment
     * @param user the signed-in user, who becomes the author
     * @return the new comment
     */
    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentView addComment(@PathVariable Long id, @RequestBody CommentRequest request,
                                  @AuthenticationPrincipal OidcUser user) {
        return projects.addComment(id, request.body(), currentUser.of(user));
    }
}

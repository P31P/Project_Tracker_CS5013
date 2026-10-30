package tracker.projects;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
import tracker.projects.ProjectDtos.IdeaRequest;
import tracker.projects.ProjectDtos.IdeaView;
import tracker.projects.ProjectDtos.ProjectDetail;

/** The ideas API under {@code /api/ideas}. Errors come back as {@code {"error": "..."}}. */
@RestController
@RequestMapping("/api/ideas")
public class IdeaController {

    private final IdeaService ideas;
    private final CurrentUser currentUser;

    /**
     * Creates the controller.
     *
     * @param ideas does the work
     * @param currentUser finds the signed-in user
     */
    public IdeaController(IdeaService ideas, CurrentUser currentUser) {
        this.ideas = ideas;
        this.currentUser = currentUser;
    }

    /**
     * Lists every idea, newest first. Handles {@code GET /api/ideas}.
     *
     * @param user the signed-in user, to mark their votes
     * @return all ideas
     */
    @GetMapping
    public List<IdeaView> list(@AuthenticationPrincipal OidcUser user) {
        return ideas.list(currentUser.of(user));
    }

    /**
     * Saves an idea. Handles {@code POST /api/ideas} and answers {@code 201}.
     *
     * @param request the idea; only the title is required
     * @param user the signed-in user, who becomes the author
     * @return the saved idea
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IdeaView create(@RequestBody IdeaRequest request, @AuthenticationPrincipal OidcUser user) {
        return ideas.create(request, currentUser.of(user));
    }

    /**
     * Votes for an idea; voting twice counts once. Handles {@code PUT /api/ideas/{id}/vote}.
     *
     * @param id the idea's id
     * @param user the signed-in user
     * @return the idea with its new vote count
     */
    @PutMapping("/{id}/vote")
    public IdeaView vote(@PathVariable Long id, @AuthenticationPrincipal OidcUser user) {
        return ideas.vote(id, currentUser.of(user));
    }

    /**
     * Takes back a vote. Handles {@code DELETE /api/ideas/{id}/vote}.
     *
     * @param id the idea's id
     * @param user the signed-in user
     * @return the idea with its new vote count
     */
    @DeleteMapping("/{id}/vote")
    public IdeaView unvote(@PathVariable Long id, @AuthenticationPrincipal OidcUser user) {
        return ideas.unvote(id, currentUser.of(user));
    }

    /**
     * Turns an idea into a Planning project. Handles {@code POST /api/ideas/{id}/promote} and answers {@code 201}.
     *
     * @param id the idea's id
     * @param user the signed-in user, who becomes the project's owner
     * @return the new project
     */
    @PostMapping("/{id}/promote")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectDetail promote(@PathVariable Long id, @AuthenticationPrincipal OidcUser user) {
        return ideas.promote(id, currentUser.of(user));
    }

    /**
     * Lists an idea's discussion, oldest first. Handles {@code GET /api/ideas/{id}/comments}.
     *
     * @param id the idea's id
     * @return the comments
     */
    @GetMapping("/{id}/comments")
    public List<CommentView> comments(@PathVariable Long id) {
        return ideas.comments(id);
    }

    /**
     * Adds a comment to an idea's discussion. Handles {@code POST /api/ideas/{id}/comments} and answers {@code 201}.
     *
     * @param id the idea's id
     * @param request the comment
     * @param user the signed-in user, who becomes the author
     * @return the new comment
     */
    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentView addComment(@PathVariable Long id, @RequestBody CommentRequest request,
                                  @AuthenticationPrincipal OidcUser user) {
        return ideas.addComment(id, request.body(), currentUser.of(user));
    }
}

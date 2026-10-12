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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tracker.auth.CurrentUser;
import tracker.projects.ProjectDtos.LinkRequest;
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
     * @return the project
     */
    @GetMapping("/{id}")
    public ProjectDetail get(@PathVariable Long id) {
        return projects.get(id);
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
     * @return the project with the new link
     */
    @PostMapping("/{id}/links")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectDetail addLink(@PathVariable Long id, @RequestBody LinkRequest request) {
        return projects.addLink(id, request);
    }

    /**
     * Removes a link from a project. Handles {@code DELETE /api/projects/{id}/links/{linkId}}.
     *
     * @param id the project's id
     * @param linkId the link's id
     * @return the project without the link
     */
    @DeleteMapping("/{id}/links/{linkId}")
    public ProjectDetail removeLink(@PathVariable Long id, @PathVariable Long linkId) {
        return projects.removeLink(id, linkId);
    }
}

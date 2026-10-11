package tracker.projects;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tracker.auth.CurrentUser;
import tracker.projects.ProjectDtos.ProjectDetail;
import tracker.projects.ProjectDtos.ProjectRequest;
import tracker.projects.ProjectDtos.ProjectSummary;
import tracker.projects.ProjectDtos.TimelineEntry;
import tracker.projects.ProjectDtos.UpdateRequest;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projects;
    private final CurrentUser currentUser;

    public ProjectController(ProjectService projects, CurrentUser currentUser) {
        this.projects = projects;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<ProjectSummary> list() {
        return projects.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectDetail create(@RequestBody ProjectRequest request, @AuthenticationPrincipal OidcUser user) {
        return projects.create(request, currentUser.of(user));
    }

    @GetMapping("/{id}")
    public ProjectDetail get(@PathVariable Long id) {
        return projects.get(id);
    }

    @PatchMapping("/{id}")
    public ProjectDetail update(@PathVariable Long id, @RequestBody ProjectRequest request,
                                @AuthenticationPrincipal OidcUser user) {
        return projects.update(id, request, currentUser.of(user));
    }

    @PostMapping("/{id}/updates")
    @ResponseStatus(HttpStatus.CREATED)
    public TimelineEntry addUpdate(@PathVariable Long id, @RequestBody UpdateRequest request,
                                   @AuthenticationPrincipal OidcUser user) {
        return projects.addUpdate(id, request.body(), currentUser.of(user));
    }
}

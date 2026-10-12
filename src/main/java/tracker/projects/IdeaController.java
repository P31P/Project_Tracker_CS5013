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
import tracker.projects.ProjectDtos.IdeaRequest;
import tracker.projects.ProjectDtos.IdeaView;
import tracker.projects.ProjectDtos.ProjectDetail;

@RestController
@RequestMapping("/api/ideas")
public class IdeaController {

    private final IdeaService ideas;
    private final CurrentUser currentUser;

    public IdeaController(IdeaService ideas, CurrentUser currentUser) {
        this.ideas = ideas;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<IdeaView> list(@AuthenticationPrincipal OidcUser user) {
        return ideas.list(currentUser.of(user));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IdeaView create(@RequestBody IdeaRequest request, @AuthenticationPrincipal OidcUser user) {
        return ideas.create(request, currentUser.of(user));
    }

    @PutMapping("/{id}/vote")
    public IdeaView vote(@PathVariable Long id, @AuthenticationPrincipal OidcUser user) {
        return ideas.vote(id, currentUser.of(user));
    }

    @DeleteMapping("/{id}/vote")
    public IdeaView unvote(@PathVariable Long id, @AuthenticationPrincipal OidcUser user) {
        return ideas.unvote(id, currentUser.of(user));
    }

    @PostMapping("/{id}/promote")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectDetail promote(@PathVariable Long id, @AuthenticationPrincipal OidcUser user) {
        return ideas.promote(id, currentUser.of(user));
    }
}

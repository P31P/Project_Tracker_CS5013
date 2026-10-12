package tracker.projects;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tracker.auth.CurrentUser;
import tracker.data.AppUser;
import tracker.data.Idea;
import tracker.data.IdeaRepository;
import tracker.data.ProjectRepository;
import tracker.data.ProjectStatus;
import tracker.projects.ProjectDtos.IdeaView;
import tracker.projects.ProjectDtos.ProjectDetail;
import tracker.projects.ProjectDtos.ProjectRequest;

@Service
@Transactional
public class IdeaService {

    private final IdeaRepository ideas;
    private final ProjectRepository projects;
    private final ProjectService projectService;

    public IdeaService(IdeaRepository ideas, ProjectRepository projects, ProjectService projectService) {
        this.ideas = ideas;
        this.projects = projects;
        this.projectService = projectService;
    }

    @Transactional(readOnly = true)
    public List<IdeaView> list() {
        return ideas.findAllByOrderByCreatedAtDescIdDesc().stream().map(IdeaService::view).toList();
    }

    public IdeaView create(String title, String description, AppUser author) {
        Idea idea = new Idea(ProjectService.required(title, "An idea needs a title"),
                ProjectService.blankToNull(description), author);
        return view(ideas.save(idea));
    }

    /** Starts a project from the idea, in Planning, and links the two. An idea becomes a project only once. */
    public ProjectDetail promote(Long id, AppUser actor) {
        Idea idea = ideas.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No idea with id " + id));
        if (idea.getProject() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This idea is already a project");
        }
        ProjectDetail project = projectService.create(
                new ProjectRequest(idea.getTitle(), idea.getDescription(), ProjectStatus.PLANNING, null, null), actor);
        idea.promotedTo(projects.getReferenceById(project.id()));
        return project;
    }

    private static IdeaView view(Idea idea) {
        return new IdeaView(idea.getId(), idea.getTitle(), idea.getDescription(), CurrentUser.nameOf(idea.getAuthor()),
                idea.getCreatedAt(),
                idea.getProject() == null ? null : idea.getProject().getId(),
                idea.getProject() == null ? null : idea.getProject().getTitle());
    }
}

package tracker.projects;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tracker.auth.CurrentUser;
import tracker.data.AppUser;
import tracker.data.AppUserRepository;
import tracker.data.Idea;
import tracker.data.IdeaRepository;
import tracker.data.ProjectRepository;
import tracker.data.ProjectStatus;
import tracker.projects.ProjectDtos.IdeaRequest;
import tracker.projects.ProjectDtos.IdeaView;
import tracker.projects.ProjectDtos.ProjectDetail;
import tracker.projects.ProjectDtos.ProjectRequest;

/** Saves ideas, records votes and turns ideas into projects. */
@Service
@Transactional
public class IdeaService {

    private final IdeaRepository ideas;
    private final ProjectRepository projects;
    private final AppUserRepository users;
    private final ProjectService projectService;

    /**
     * Creates the service.
     *
     * @param ideas where ideas are stored
     * @param projects where projects are stored
     * @param users where users are stored
     * @param projectService creates the project when an idea is promoted
     */
    public IdeaService(IdeaRepository ideas, ProjectRepository projects, AppUserRepository users,
                       ProjectService projectService) {
        this.ideas = ideas;
        this.projects = projects;
        this.users = users;
        this.projectService = projectService;
    }

    /**
     * Lists every idea, newest first.
     *
     * @param viewer the signed-in user, to say which ideas they voted for; may be {@code null}
     * @return all ideas
     */
    @Transactional(readOnly = true)
    public List<IdeaView> list(AppUser viewer) {
        return ideas.findAllByOrderByCreatedAtDescIdDesc().stream().map(idea -> view(idea, viewer)).toList();
    }

    /**
     * Saves an idea.
     *
     * @param request the idea
     * @param author who wrote it; may be {@code null}
     * @return the saved idea
     * @throws ResponseStatusException {@code 400} if the title is blank
     */
    public IdeaView create(IdeaRequest request, AppUser author) {
        Idea idea = new Idea(ProjectService.required(request.title(), "An idea needs a title"),
                ProjectService.blankToNull(request.description()), author);
        idea.setTags(ProjectService.tags(request.tags()));
        return view(ideas.save(idea), author);
    }

    /**
     * Records that a user is interested in an idea. Voting twice counts once.
     *
     * @param id the idea's id
     * @param voter who is voting; if {@code null}, nothing is recorded
     * @return the idea with its new vote count
     * @throws ResponseStatusException {@code 404} if there is no idea with this id
     */
    public IdeaView vote(Long id, AppUser voter) {
        Idea idea = find(id);
        if (voter != null && !votedBy(idea, voter)) {
            idea.getVoters().add(users.getReferenceById(voter.getId()));
        }
        return view(idea, voter);
    }

    /**
     * Takes back a user's vote. Does nothing if they hadn't voted.
     *
     * @param id the idea's id
     * @param voter whose vote to remove; if {@code null}, nothing changes
     * @return the idea with its new vote count
     * @throws ResponseStatusException {@code 404} if there is no idea with this id
     */
    public IdeaView unvote(Long id, AppUser voter) {
        Idea idea = find(id);
        if (voter != null) {
            idea.getVoters().removeIf(user -> user.getId().equals(voter.getId()));
        }
        return view(idea, voter);
    }

    /**
     * Starts a project from an idea, in Planning and with the idea's tags, and links the two. An idea becomes a
     * project only once.
     *
     * @param id the idea's id
     * @param actor who is promoting it; becomes the project's owner
     * @return the new project
     * @throws ResponseStatusException {@code 404} if there is no such idea, or {@code 409} if it
     *         already became a project
     */
    public ProjectDetail promote(Long id, AppUser actor) {
        Idea idea = find(id);
        if (idea.getProject() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This idea is already a project");
        }
        ProjectDetail project = projectService.create(new ProjectRequest(idea.getTitle(), idea.getDescription(),
                ProjectStatus.PLANNING, idea.getTags(), null), actor);
        idea.promotedTo(projects.getReferenceById(project.id()));
        return project;
    }

    private Idea find(Long id) {
        return ideas.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No idea with id " + id));
    }

    // the viewer is loaded outside this transaction, so compare by id rather than by object
    private static boolean votedBy(Idea idea, AppUser viewer) {
        return viewer != null && idea.getVoters().stream().anyMatch(user -> user.getId().equals(viewer.getId()));
    }

    private static IdeaView view(Idea idea, AppUser viewer) {
        return new IdeaView(idea.getId(), idea.getTitle(), idea.getDescription(), CurrentUser.nameOf(idea.getAuthor()),
                idea.getTags(), idea.getCreatedAt(),
                idea.getProject() == null ? null : idea.getProject().getId(),
                idea.getProject() == null ? null : idea.getProject().getTitle(),
                idea.getVoters().size(), votedBy(idea, viewer));
    }
}

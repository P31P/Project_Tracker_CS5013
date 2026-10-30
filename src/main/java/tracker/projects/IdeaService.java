package tracker.projects;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tracker.auth.CurrentUser;
import tracker.data.AppUser;
import tracker.data.AppUserRepository;
import tracker.data.Idea;
import tracker.data.IdeaComment;
import tracker.data.IdeaCommentRepository;
import tracker.data.IdeaRepository;
import tracker.data.ProjectRepository;
import tracker.data.ProjectStatus;
import tracker.projects.ProjectDtos.CommentView;
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
    private final IdeaCommentRepository comments;

    /**
     * Creates the service.
     *
     * @param ideas where ideas are stored
     * @param projects where projects are stored
     * @param users where users are stored
     * @param projectService creates the project when an idea is promoted
     * @param comments where the ideas' discussions are stored
     */
    public IdeaService(IdeaRepository ideas, ProjectRepository projects, AppUserRepository users,
                       ProjectService projectService, IdeaCommentRepository comments) {
        this.ideas = ideas;
        this.projects = projects;
        this.users = users;
        this.projectService = projectService;
        this.comments = comments;
    }

    /**
     * Lists every idea, newest first.
     *
     * @param viewer the signed-in user, to say which ideas they voted for; may be {@code null}
     * @return all ideas
     */
    @Transactional(readOnly = true)
    public List<IdeaView> list(AppUser viewer) {
        Map<Long, Long> commentCounts = new HashMap<>();
        for (Object[] row : comments.countPerIdea()) {
            commentCounts.put((Long) row[0], (Long) row[1]);
        }
        return ideas.findAllByOrderByCreatedAtDescIdDesc().stream()
                .map(idea -> view(idea, viewer, commentCounts.getOrDefault(idea.getId(), 0L)))
                .toList();
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
        return view(ideas.save(idea), author, 0);
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
        return view(idea, voter, comments.countByIdeaId(id));
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
        return view(idea, voter, comments.countByIdeaId(id));
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

    /**
     * Lists an idea's discussion, oldest first.
     *
     * @param id the idea's id
     * @return the comments
     * @throws ResponseStatusException {@code 404} if there is no idea with this id
     */
    @Transactional(readOnly = true)
    public List<CommentView> comments(Long id) {
        find(id);
        return comments.findByIdeaIdOrderByCreatedAtAscIdAsc(id).stream().map(IdeaService::commentView).toList();
    }

    /**
     * Adds a comment to an idea's discussion.
     *
     * @param id the idea's id
     * @param body the comment
     * @param author who wrote it; may be {@code null}
     * @return the new comment
     * @throws ResponseStatusException {@code 404} if there is no such idea, or {@code 400} if the comment is blank
     */
    public CommentView addComment(Long id, String body, AppUser author) {
        Idea idea = find(id);
        IdeaComment comment = new IdeaComment(idea, author, ProjectService.required(body, "A comment can't be empty"));
        return commentView(comments.save(comment));
    }

    private Idea find(Long id) {
        return ideas.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No idea with id " + id));
    }

    // the viewer is loaded outside this transaction, so compare by id rather than by object
    private static boolean votedBy(Idea idea, AppUser viewer) {
        return viewer != null && idea.getVoters().stream().anyMatch(user -> user.getId().equals(viewer.getId()));
    }

    private static IdeaView view(Idea idea, AppUser viewer, long commentCount) {
        return new IdeaView(idea.getId(), idea.getTitle(), idea.getDescription(), CurrentUser.nameOf(idea.getAuthor()),
                idea.getTags(), idea.getCreatedAt(),
                idea.getProject() == null ? null : idea.getProject().getId(),
                idea.getProject() == null ? null : idea.getProject().getTitle(),
                idea.getVoters().size(), votedBy(idea, viewer), commentCount);
    }

    private static CommentView commentView(IdeaComment comment) {
        return new CommentView(comment.getId(), comment.getBody(), CurrentUser.nameOf(comment.getAuthor()),
                comment.getCreatedAt());
    }
}

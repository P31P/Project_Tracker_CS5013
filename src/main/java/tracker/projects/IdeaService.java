package tracker.projects;

import java.util.ArrayList;
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
import tracker.data.IdeaLink;
import tracker.data.IdeaLinkRepository;
import tracker.data.PaperRepository;
import tracker.data.IdeaRepository;
import tracker.data.ProjectRepository;
import tracker.data.ProjectStatus;
import tracker.projects.ProjectDtos.CommentView;
import tracker.projects.ProjectDtos.IdeaRequest;
import tracker.projects.ProjectDtos.IdeaView;
import tracker.projects.ProjectDtos.LinkRequest;
import tracker.projects.ProjectDtos.LinkView;
import tracker.projects.ProjectDtos.ProjectDetail;
import tracker.projects.ProjectDtos.ProjectRequest;

/** Saves ideas, records votes, keeps their references and turns ideas into projects. */
@Service
@Transactional
public class IdeaService {

    private final IdeaRepository ideas;
    private final ProjectRepository projects;
    private final AppUserRepository users;
    private final ProjectService projectService;
    private final IdeaCommentRepository comments;
    private final IdeaLinkRepository links;
    private final PaperRepository papers;

    /**
     * Creates the service.
     *
     * @param ideas where ideas are stored
     * @param projects where projects are stored
     * @param users where users are stored
     * @param projectService creates the project when an idea is promoted
     * @param comments where the ideas' discussions are stored
     * @param links where the ideas' web links are stored
     * @param papers where the archive's papers are stored
     */
    public IdeaService(IdeaRepository ideas, ProjectRepository projects, AppUserRepository users,
                       ProjectService projectService, IdeaCommentRepository comments, IdeaLinkRepository links,
                       PaperRepository papers) {
        this.ideas = ideas;
        this.projects = projects;
        this.users = users;
        this.projectService = projectService;
        this.comments = comments;
        this.links = links;
        this.papers = papers;
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
        Map<Long, List<LinkView>> linksByIdea = new HashMap<>();
        for (IdeaLink link : links.findAllByOrderById()) {
            linksByIdea.computeIfAbsent(link.getIdea().getId(), id -> new ArrayList<>()).add(linkView(link));
        }
        return ideas.findAllByOrderByCreatedAtDescIdDesc().stream()
                .map(idea -> view(idea, viewer, commentCounts.getOrDefault(idea.getId(), 0L),
                        linksByIdea.getOrDefault(idea.getId(), List.of())))
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
        if (request.details() != null) {
            idea.setDetails(ProjectService.details(request.details()));
        }
        idea.setOpenForCollab(Boolean.TRUE.equals(request.openForCollab()));
        return view(ideas.save(idea), author, 0, List.of());
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
        return full(idea, voter);
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
        return full(idea, voter);
    }

    /**
     * Starts a project from an idea, in Planning and with the idea's tags, write-up and collaboration status, and
     * links the two. An idea becomes a
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
                ProjectStatus.PLANNING, idea.getTags(), null, null, ProjectService.view(idea.getDetails()),
                idea.isOpenForCollab()), actor);
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

    /**
     * Adds a web link to an idea's references. Without a label, the site's name is used.
     *
     * @param id the idea's id
     * @param request the link
     * @param viewer the signed-in user
     * @return the idea with the new link
     * @throws ResponseStatusException {@code 404} if there is no such idea, or {@code 400} if the address is
     *         missing or isn't an http(s) web address
     */
    public IdeaView addLink(Long id, LinkRequest request, AppUser viewer) {
        Idea idea = find(id);
        LinkView link = ProjectService.checkedLink(request);
        links.save(new IdeaLink(idea, link.label(), link.url()));
        return full(idea, viewer);
    }

    /**
     * Removes a web link from an idea.
     *
     * @param id the idea's id
     * @param linkId the link's id
     * @param viewer the signed-in user
     * @return the idea without the link
     * @throws ResponseStatusException {@code 404} if there is no such idea or no such link on it
     */
    public IdeaView removeLink(Long id, Long linkId, AppUser viewer) {
        Idea idea = find(id);
        IdeaLink link = links.findByIdAndIdeaId(linkId, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No link with id " + linkId));
        links.delete(link);
        return full(idea, viewer);
    }

    /**
     * Lists a paper from the archive as one of the idea's references. Adding it twice changes nothing.
     *
     * @param id the idea's id
     * @param paperId the paper's id
     * @param viewer the signed-in user
     * @return the idea with the paper
     * @throws ResponseStatusException {@code 404} if there is no such idea, {@code 400} if there is no such paper
     */
    public IdeaView addPaper(Long id, Long paperId, AppUser viewer) {
        Idea idea = find(id);
        idea.getPapers().add(ProjectService.paper(papers, paperId));
        return full(idea, viewer);
    }

    /**
     * Takes a paper off the idea's references.
     *
     * @param id the idea's id
     * @param paperId the paper's id
     * @param viewer the signed-in user
     * @return the idea without the paper
     * @throws ResponseStatusException {@code 404} if there is no such idea
     */
    public IdeaView removePaper(Long id, Long paperId, AppUser viewer) {
        Idea idea = find(id);
        idea.getPapers().removeIf(paper -> paper.getId().equals(paperId));
        return full(idea, viewer);
    }

    // one idea with its comment count and links, after a change to it
    private IdeaView full(Idea idea, AppUser viewer) {
        return view(idea, viewer, comments.countByIdeaId(idea.getId()),
                links.findByIdeaIdOrderById(idea.getId()).stream().map(IdeaService::linkView).toList());
    }

    private static LinkView linkView(IdeaLink link) {
        return new LinkView(link.getId(), link.getLabel(), link.getUrl());
    }

    private Idea find(Long id) {
        return ideas.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No idea with id " + id));
    }

    // the viewer is loaded outside this transaction, so compare by id rather than by object
    private static boolean votedBy(Idea idea, AppUser viewer) {
        return viewer != null && idea.getVoters().stream().anyMatch(user -> user.getId().equals(viewer.getId()));
    }

    private static IdeaView view(Idea idea, AppUser viewer, long commentCount, List<LinkView> ideaLinks) {
        return new IdeaView(idea.getId(), idea.getTitle(), idea.getDescription(), CurrentUser.nameOf(idea.getAuthor()),
                idea.getTags(), idea.getCreatedAt(),
                idea.getProject() == null ? null : idea.getProject().getId(),
                idea.getProject() == null ? null : idea.getProject().getTitle(),
                idea.getVoters().size(), votedBy(idea, viewer), commentCount, ProjectService.view(idea.getDetails()),
                idea.isOpenForCollab(), ideaLinks, ProjectService.paperRefs(idea.getPapers()),
                idea.getAuthor() == null ? null : idea.getAuthor().getId());
    }

    private static CommentView commentView(IdeaComment comment) {
        return new CommentView(comment.getId(), comment.getBody(), CurrentUser.nameOf(comment.getAuthor()),
                comment.getCreatedAt());
    }
}

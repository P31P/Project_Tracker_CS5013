package tracker.projects;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tracker.auth.CurrentUser;
import tracker.data.AppUser;
import tracker.data.AppUserRepository;
import tracker.data.Project;
import tracker.data.ProjectLink;
import tracker.data.ProjectLinkRepository;
import tracker.data.ProjectRepository;
import tracker.data.ProjectUpdate;
import tracker.data.ProjectUpdateRepository;
import tracker.data.ResearchDetails;
import tracker.data.UpdateKind;
import tracker.projects.ProjectDtos.Details;
import tracker.projects.ProjectDtos.LinkRequest;
import tracker.projects.ProjectDtos.LinkView;
import tracker.projects.ProjectDtos.Person;
import tracker.projects.ProjectDtos.ProjectDetail;
import tracker.projects.ProjectDtos.ProjectRequest;
import tracker.projects.ProjectDtos.ProjectSummary;
import tracker.projects.ProjectDtos.TimelineEntry;

/**
 * Creates, changes and reads projects, their timelines, members, tags and links.
 *
 * <p>Errors are thrown as {@link ResponseStatusException}s with a readable reason.
 */
@Service
@Transactional
public class ProjectService {

    static final int MAX_DETAIL = 5000;

    private final ProjectRepository projects;
    private final ProjectUpdateRepository updates;
    private final ProjectLinkRepository links;
    private final AppUserRepository users;

    /**
     * Creates the service.
     *
     * @param projects where projects are stored
     * @param updates where timeline entries are stored
     * @param links where project links are stored
     * @param users where users are stored
     */
    public ProjectService(ProjectRepository projects, ProjectUpdateRepository updates, ProjectLinkRepository links,
                          AppUserRepository users) {
        this.projects = projects;
        this.updates = updates;
        this.links = links;
        this.users = users;
    }

    /**
     * Lists every project, most recently changed first.
     *
     * @return all projects
     */
    @Transactional(readOnly = true)
    public List<ProjectSummary> list() {
        return summaries(project -> true);
    }

    /**
     * Lists the projects a user owns or is a member of, most recently changed first.
     *
     * @param user the user; may be {@code null}
     * @return the user's projects, or an empty list if {@code user} is {@code null}
     */
    @Transactional(readOnly = true)
    public List<ProjectSummary> involving(AppUser user) {
        if (user == null) {
            return List.of();
        }
        return summaries(project -> isUser(project.getOwner(), user)
                || project.getMembers().stream().anyMatch(member -> isUser(member, user)));
    }

    /**
     * Returns one project with its timeline and links.
     *
     * @param id the project's id
     * @return the project
     * @throws ResponseStatusException {@code 404} if there is no project with this id
     */
    @Transactional(readOnly = true)
    public ProjectDetail get(Long id) {
        return detail(find(id));
    }

    /**
     * Creates a project. Only the title is required; the owner becomes its first member.
     *
     * @param request the project to create
     * @param owner who is creating it; may be {@code null}
     * @return the new project
     * @throws ResponseStatusException {@code 400} if the title is blank or a member id is unknown
     */
    public ProjectDetail create(ProjectRequest request, AppUser owner) {
        Project project = new Project(required(request.title(), "A project needs a title"));
        project.setDescription(blankToNull(request.description()));
        if (request.status() != null) {
            project.setStatus(request.status());
        }
        project.setTags(tags(request.tags()));
        if (request.startedOn() != null) {
            project.setStartedOn(request.startedOn());
        }
        if (request.details() != null) {
            project.setDetails(details(request.details()));
        }
        project.setOpenForCollab(Boolean.TRUE.equals(request.openForCollab()));
        project.setOwner(owner);
        if (owner != null) {
            project.getMembers().add(users.getReferenceById(owner.getId()));
        }
        if (request.memberIds() != null) {
            project.getMembers().addAll(members(request.memberIds()));
        }
        return detail(projects.save(project));
    }

    /**
     * Changes a project. Only the fields in the request are changed, and a status change is logged on the timeline.
     *
     * @param id the project's id
     * @param request the changes
     * @param actor who is making them; may be {@code null}
     * @return the changed project
     * @throws ResponseStatusException {@code 404} if there is no such project, or {@code 400} if
     *         the title is blank or a member id is unknown
     */
    public ProjectDetail update(Long id, ProjectRequest request, AppUser actor) {
        Project project = find(id);
        if (request.title() != null) {
            project.setTitle(required(request.title(), "A project needs a title"));
        }
        if (request.description() != null) {
            project.setDescription(blankToNull(request.description()));
        }
        if (request.tags() != null) {
            project.setTags(tags(request.tags()));
        }
        if (request.startedOn() != null) {
            project.setStartedOn(request.startedOn());
        }
        if (request.details() != null) {
            project.setDetails(details(request.details()));
        }
        if (request.openForCollab() != null) {
            project.setOpenForCollab(request.openForCollab());
        }
        if (request.memberIds() != null) {
            List<AppUser> members = members(request.memberIds());
            project.getMembers().clear();
            project.getMembers().addAll(members);
        }
        if (request.status() != null && request.status() != project.getStatus()) {
            String change = "Status changed from " + project.getStatus().label() + " to " + request.status().label();
            updates.save(new ProjectUpdate(project, actor, UpdateKind.STATUS, change));
            project.setStatus(request.status());
        }
        project.touch();
        return detail(project);
    }

    /**
     * Adds a note to a project's timeline.
     *
     * @param id the project's id
     * @param body the note
     * @param author who wrote it; may be {@code null}
     * @return the new timeline entry
     * @throws ResponseStatusException {@code 404} if there is no such project, or {@code 400} if
     *         the note is blank
     */
    public TimelineEntry addUpdate(Long id, String body, AppUser author) {
        Project project = find(id);
        ProjectUpdate update = updates.save(
                new ProjectUpdate(project, author, UpdateKind.NOTE, required(body, "An update can't be empty")));
        project.touch();
        return entry(update);
    }

    /**
     * Adds a link such as a repository or a document. Without a label, the site's name is used.
     *
     * @param id the project's id
     * @param request the link
     * @return the project with the new link
     * @throws ResponseStatusException {@code 404} if there is no such project, or {@code 400} if
     *         the address is missing or isn't an http(s) web address
     */
    public ProjectDetail addLink(Long id, LinkRequest request) {
        Project project = find(id);
        String url = required(request.url(), "A link needs a URL");
        String host = webHost(url);
        String label = request.label() == null || request.label().isBlank()
                ? host.replaceFirst("^www\\.", "")
                : request.label().trim();
        links.save(new ProjectLink(project, label, url));
        project.touch();
        return detail(project);
    }

    /**
     * Removes a link from a project.
     *
     * @param id the project's id
     * @param linkId the link's id
     * @return the project without the link
     * @throws ResponseStatusException {@code 404} if there is no such project or no such link
     *         on it
     */
    public ProjectDetail removeLink(Long id, Long linkId) {
        Project project = find(id);
        ProjectLink link = links.findByIdAndProjectId(linkId, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No link with id " + linkId));
        links.delete(link);
        project.touch();
        return detail(project);
    }

    private List<ProjectSummary> summaries(Predicate<Project> keep) {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : updates.countPerProject()) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return projects.findAllByOrderByUpdatedAtDescIdDesc().stream()
                .filter(keep)
                .map(p -> new ProjectSummary(p.getId(), p.getTitle(), p.getDescription(), p.getStatus(),
                        CurrentUser.nameOf(p.getOwner()), p.getTags(), people(p.getMembers()), p.getCreatedAt(),
                        p.getUpdatedAt(), counts.getOrDefault(p.getId(), 0L), p.getStartedOn(), p.isOpenForCollab()))
                .toList();
    }

    private Project find(Long id) {
        return projects.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No project with id " + id));
    }

    private List<AppUser> members(List<Long> ids) {
        List<AppUser> found = users.findAllById(ids);
        if (found.size() != new HashSet<>(ids).size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Someone in the member list doesn't exist");
        }
        return found;
    }

    private ProjectDetail detail(Project p) {
        List<TimelineEntry> timeline = updates.findByProjectIdOrderByCreatedAtDescIdDesc(p.getId()).stream()
                .map(ProjectService::entry)
                .toList();
        List<LinkView> linkViews = links.findByProjectIdOrderById(p.getId()).stream()
                .map(link -> new LinkView(link.getId(), link.getLabel(), link.getUrl()))
                .toList();
        return new ProjectDetail(p.getId(), p.getTitle(), p.getDescription(), p.getStatus(),
                CurrentUser.nameOf(p.getOwner()), p.getTags(), people(p.getMembers()), linkViews, p.getCreatedAt(),
                p.getUpdatedAt(), timeline, p.getStartedOn(), view(p.getDetails()), p.isOpenForCollab(),
                p.getCollabOpenedAt());
    }

    /**
     * Tidies a write-up: each part is trimmed, and a blank part becomes none.
     *
     * @param request the five parts as sent
     * @return the details to store
     * @throws ResponseStatusException {@code 400} if a part is longer than 5000 characters
     */
    static ResearchDetails details(Details request) {
        return new ResearchDetails(detail(request.coreQuestion(), "core question"),
                detail(request.motivation(), "motivation"), detail(request.approach(), "approach"),
                detail(request.resources(), "data and resources"), detail(request.challenges(), "challenges"));
    }

    /**
     * Turns stored details into what the API returns.
     *
     * @param details the stored details
     * @return the same five parts
     */
    static Details view(ResearchDetails details) {
        return new Details(details.getCoreQuestion(), details.getMotivation(), details.getApproach(),
                details.getResources(), details.getChallenges());
    }

    private static String detail(String text, String what) {
        String tidied = blankToNull(text);
        if (tidied != null && tidied.length() > MAX_DETAIL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The " + what + " can be at most " + MAX_DETAIL + " characters");
        }
        return tidied;
    }

    private static TimelineEntry entry(ProjectUpdate u) {
        return new TimelineEntry(u.getId(), u.getKind(), u.getBody(), CurrentUser.nameOf(u.getAuthor()),
                u.getCreatedAt());
    }

    /**
     * Turns users into {@link Person}s, sorted by name.
     *
     * @param users the users
     * @return the same people, by name
     */
    static List<Person> people(Collection<AppUser> users) {
        return users.stream()
                .map(user -> new Person(user.getId(), CurrentUser.nameOf(user)))
                .sorted(Comparator.comparing(Person::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    // the signed-in user is loaded outside this transaction, so compare by id rather than by object
    private static boolean isUser(AppUser candidate, AppUser user) {
        return candidate != null && candidate.getId().equals(user.getId());
    }

    /**
     * Tidies tags: trims them, drops a leading {@code #}, lowercases them and removes blanks and duplicates.
     * For example, {@code ["  #Vision", "NLP", "vision"]} becomes {@code ["vision", "nlp"]}.
     *
     * @param raw the tags as typed; may be {@code null}
     * @return the tidied tags, in their first order
     */
    public static List<String> tags(List<String> raw) {
        if (raw == null) {
            return List.of();
        }
        return raw.stream()
                .filter(Objects::nonNull)
                .map(tag -> tag.trim().replaceFirst("^#", "").toLowerCase(Locale.ROOT))
                .filter(tag -> !tag.isEmpty())
                .distinct()
                .toList();
    }

    private static String webHost(String url) {
        try {
            URI uri = new URI(url);
            if (uri.getHost() != null && ("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme()))) {
                return uri.getHost();
            }
        } catch (URISyntaxException e) {
            // falls through to the error below
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A link must be a web address starting with https://");
    }

    /**
     * Checks that a required text was given.
     *
     * @param text the text
     * @param message the error message if it is missing
     * @return the text, trimmed
     * @throws ResponseStatusException {@code 400} with {@code message} if the
     *         text is {@code null} or blank
     */
    static String required(String text, String message) {
        if (text == null || text.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return text.trim();
    }

    /**
     * Trims an optional text, treating a blank one as missing.
     *
     * @param text the text; may be {@code null}
     * @return the trimmed text, or {@code null} if it was {@code null} or blank
     */
    static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}

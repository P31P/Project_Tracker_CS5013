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
import tracker.data.UpdateKind;
import tracker.projects.ProjectDtos.LinkRequest;
import tracker.projects.ProjectDtos.LinkView;
import tracker.projects.ProjectDtos.Person;
import tracker.projects.ProjectDtos.ProjectDetail;
import tracker.projects.ProjectDtos.ProjectRequest;
import tracker.projects.ProjectDtos.ProjectSummary;
import tracker.projects.ProjectDtos.TimelineEntry;

@Service
@Transactional
public class ProjectService {

    private final ProjectRepository projects;
    private final ProjectUpdateRepository updates;
    private final ProjectLinkRepository links;
    private final AppUserRepository users;

    public ProjectService(ProjectRepository projects, ProjectUpdateRepository updates, ProjectLinkRepository links,
                          AppUserRepository users) {
        this.projects = projects;
        this.updates = updates;
        this.links = links;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<ProjectSummary> list() {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : updates.countPerProject()) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return projects.findAllByOrderByUpdatedAtDescIdDesc().stream()
                .map(p -> new ProjectSummary(p.getId(), p.getTitle(), p.getDescription(), p.getStatus(),
                        CurrentUser.nameOf(p.getOwner()), p.getTags(), people(p.getMembers()), p.getCreatedAt(),
                        p.getUpdatedAt(), counts.getOrDefault(p.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectDetail get(Long id) {
        return detail(find(id));
    }

    public ProjectDetail create(ProjectRequest request, AppUser owner) {
        Project project = new Project(required(request.title(), "A project needs a title"));
        project.setDescription(blankToNull(request.description()));
        if (request.status() != null) {
            project.setStatus(request.status());
        }
        project.setTags(tags(request.tags()));
        project.setOwner(owner);
        if (owner != null) {
            project.getMembers().add(users.getReferenceById(owner.getId()));
        }
        if (request.memberIds() != null) {
            project.getMembers().addAll(members(request.memberIds()));
        }
        return detail(projects.save(project));
    }

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

    public TimelineEntry addUpdate(Long id, String body, AppUser author) {
        Project project = find(id);
        ProjectUpdate update = updates.save(
                new ProjectUpdate(project, author, UpdateKind.NOTE, required(body, "An update can't be empty")));
        project.touch();
        return entry(update);
    }

    /** Adds a link such as a repo or a doc. Without a label, the site's name is used. */
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

    public ProjectDetail removeLink(Long id, Long linkId) {
        Project project = find(id);
        ProjectLink link = links.findByIdAndProjectId(linkId, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No link with id " + linkId));
        links.delete(link);
        project.touch();
        return detail(project);
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
                p.getUpdatedAt(), timeline);
    }

    private static TimelineEntry entry(ProjectUpdate u) {
        return new TimelineEntry(u.getId(), u.getKind(), u.getBody(), CurrentUser.nameOf(u.getAuthor()),
                u.getCreatedAt());
    }

    static List<Person> people(Collection<AppUser> users) {
        return users.stream()
                .map(user -> new Person(user.getId(), CurrentUser.nameOf(user)))
                .sorted(Comparator.comparing(Person::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /** "  #Vision, NLP, vision " becomes ["vision", "nlp"]. */
    static List<String> tags(List<String> raw) {
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

    static String required(String text, String message) {
        if (text == null || text.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return text.trim();
    }

    static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}

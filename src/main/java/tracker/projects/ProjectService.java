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
import tracker.data.Project;
import tracker.data.ProjectRepository;
import tracker.data.ProjectUpdate;
import tracker.data.ProjectUpdateRepository;
import tracker.data.UpdateKind;
import tracker.projects.ProjectDtos.ProjectDetail;
import tracker.projects.ProjectDtos.ProjectRequest;
import tracker.projects.ProjectDtos.ProjectSummary;
import tracker.projects.ProjectDtos.TimelineEntry;

@Service
@Transactional
public class ProjectService {

    private final ProjectRepository projects;
    private final ProjectUpdateRepository updates;

    public ProjectService(ProjectRepository projects, ProjectUpdateRepository updates) {
        this.projects = projects;
        this.updates = updates;
    }

    @Transactional(readOnly = true)
    public List<ProjectSummary> list() {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : updates.countPerProject()) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return projects.findAllByOrderByUpdatedAtDescIdDesc().stream()
                .map(p -> new ProjectSummary(p.getId(), p.getTitle(), p.getDescription(), p.getStatus(),
                        CurrentUser.nameOf(p.getOwner()), p.getCreatedAt(), p.getUpdatedAt(),
                        counts.getOrDefault(p.getId(), 0L)))
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
        project.setOwner(owner);
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

    private Project find(Long id) {
        return projects.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No project with id " + id));
    }

    private ProjectDetail detail(Project p) {
        List<TimelineEntry> timeline = updates.findByProjectIdOrderByCreatedAtDescIdDesc(p.getId()).stream()
                .map(ProjectService::entry)
                .toList();
        return new ProjectDetail(p.getId(), p.getTitle(), p.getDescription(), p.getStatus(),
                CurrentUser.nameOf(p.getOwner()), p.getCreatedAt(), p.getUpdatedAt(), timeline);
    }

    private static TimelineEntry entry(ProjectUpdate u) {
        return new TimelineEntry(u.getId(), u.getKind(), u.getBody(), CurrentUser.nameOf(u.getAuthor()),
                u.getCreatedAt());
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

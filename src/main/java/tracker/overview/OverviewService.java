package tracker.overview;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tracker.auth.CurrentUser;
import tracker.data.AppUser;
import tracker.data.IdeaRepository;
import tracker.data.PaperRepository;
import tracker.data.ProjectRepository;
import tracker.data.ProjectStatus;
import tracker.data.ProjectUpdateRepository;
import tracker.data.UpdateKind;
import tracker.projects.ProjectDtos.ProjectSummary;
import tracker.projects.ProjectService;

/** The home page: lab-wide numbers, the user's own projects and what happened lately. */
@Service
@Transactional(readOnly = true)
public class OverviewService {

    private static final int FEED_SIZE = 12;

    public record Overview(Counts counts, List<ProjectSummary> mine, List<Activity> activity) {
    }

    public record Counts(long planning, long active, long onHold, long done, long openIdeas, long papers) {
    }

    /** One line in the feed, e.g. "Priyal · posted an update on · Graph search". href is a tab link or a paper URL. */
    public record Activity(String kind, String who, String action, String target, String href, String excerpt,
                           OffsetDateTime at) {
    }

    private final ProjectRepository projects;
    private final ProjectUpdateRepository updates;
    private final IdeaRepository ideas;
    private final PaperRepository papers;
    private final ProjectService projectService;

    public OverviewService(ProjectRepository projects, ProjectUpdateRepository updates, IdeaRepository ideas,
                           PaperRepository papers, ProjectService projectService) {
        this.projects = projects;
        this.updates = updates;
        this.ideas = ideas;
        this.papers = papers;
        this.projectService = projectService;
    }

    public Overview overview(AppUser user) {
        Counts counts = new Counts(projects.countByStatus(ProjectStatus.PLANNING),
                projects.countByStatus(ProjectStatus.ACTIVE), projects.countByStatus(ProjectStatus.ON_HOLD),
                projects.countByStatus(ProjectStatus.DONE), ideas.countByProjectIsNull(), papers.count());
        return new Overview(counts, projectService.involving(user), activity());
    }

    // the latest few of each kind, merged and cut down to the newest FEED_SIZE
    private List<Activity> activity() {
        List<Activity> feed = new ArrayList<>();
        updates.findTop15ByOrderByCreatedAtDescIdDesc().forEach(u -> feed.add(new Activity(u.getKind().name(),
                CurrentUser.nameOf(u.getAuthor()),
                u.getKind() == UpdateKind.STATUS ? "changed the status of" : "posted an update on",
                u.getProject().getTitle(), "#projects/" + u.getProject().getId(), u.getBody(), u.getCreatedAt())));
        projects.findTop10ByOrderByCreatedAtDescIdDesc().forEach(p -> feed.add(new Activity("PROJECT",
                CurrentUser.nameOf(p.getOwner()), "started", p.getTitle(), "#projects/" + p.getId(), null,
                p.getCreatedAt())));
        ideas.findTop10ByOrderByCreatedAtDescIdDesc().forEach(i -> feed.add(new Activity("IDEA",
                CurrentUser.nameOf(i.getAuthor()), "shared an idea", i.getTitle(), "#ideas", null,
                i.getCreatedAt())));
        papers.findTop10ByOrderByCreatedAtDescIdDesc().forEach(p -> feed.add(new Activity("PAPER", null,
                "A paper was shared in Slack", p.getTitle() != null ? p.getTitle() : p.getNormalizedUrl(),
                p.getNormalizedUrl(), null, p.getCreatedAt())));
        feed.sort(Comparator.comparing(Activity::at).reversed());
        return feed.subList(0, Math.min(FEED_SIZE, feed.size()));
    }
}

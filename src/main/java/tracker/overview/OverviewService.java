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

/**
 * Builds the Home tab: lab-wide numbers, the user's own projects and what happened lately.
 */
@Service
@Transactional(readOnly = true)
public class OverviewService {

    private static final int FEED_SIZE = 12;

    /**
     * Everything the Home tab shows.
     *
     * @param counts lab-wide numbers
     * @param mine the signed-in user's projects, most recently changed first
     * @param activity the newest events, newest first
     */
    public record Overview(Counts counts, List<ProjectSummary> mine, List<Activity> activity) {
    }

    /**
     * Lab-wide numbers.
     *
     * @param planning projects in Planning
     * @param active active projects
     * @param onHold projects on hold
     * @param done finished projects
     * @param openIdeas ideas that haven't become projects
     * @param papers papers shared in Slack
     */
    public record Counts(long planning, long active, long onHold, long done, long openIdeas, long papers) {
    }

    /**
     * One line in the activity feed, e.g. "Priyal · posted an update on · Graph search".
     *
     * @param kind {@code NOTE}, {@code STATUS}, {@code PROJECT}, {@code IDEA} or {@code PAPER}
     * @param who who did it, or {@code null} for a paper from Slack
     * @param action what they did, e.g. "posted an update on"
     * @param target the title of the project, idea or paper
     * @param href a tab link such as {@code #projects/12}, or a paper's web address
     * @param excerpt the text of an update, or {@code null}
     * @param at when it happened
     */
    public record Activity(String kind, String who, String action, String target, String href, String excerpt,
                           OffsetDateTime at) {
    }

    private final ProjectRepository projects;
    private final ProjectUpdateRepository updates;
    private final IdeaRepository ideas;
    private final PaperRepository papers;
    private final ProjectService projectService;

    /**
     * Creates the service.
     *
     * @param projects where projects are stored
     * @param updates where timeline entries are stored
     * @param ideas where ideas are stored
     * @param papers where papers are stored
     * @param projectService finds the user's projects
     */
    public OverviewService(ProjectRepository projects, ProjectUpdateRepository updates, IdeaRepository ideas,
                           PaperRepository papers, ProjectService projectService) {
        this.projects = projects;
        this.updates = updates;
        this.ideas = ideas;
        this.papers = papers;
        this.projectService = projectService;
    }

    /**
     * Builds the Home tab for a user.
     *
     * @param user the signed-in user; may be {@code null}, which leaves out "your projects"
     * @return the numbers, the user's projects and the latest activity
     */
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

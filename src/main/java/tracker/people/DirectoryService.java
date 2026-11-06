package tracker.people;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tracker.data.AppUser;
import tracker.data.AppUserRepository;
import tracker.data.IdeaRepository;
import tracker.profile.IdeaSummary;
import tracker.profile.ProfileView;
import tracker.projects.ProjectDtos.ProjectSummary;
import tracker.projects.ProjectService;

/** The lab's members for the People tab: everyone who can sign in, and one person's page. */
@Service
@Transactional(readOnly = true)
public class DirectoryService {

    /**
     * One person in the People tab's list.
     *
     * @param id the person's id
     * @param name their name, or their email if no name is known
     * @param background their study background, or {@code null}
     * @param interests their fields of interest
     * @param projectCount how many projects they own or are a member of
     * @param ideaCount how many ideas they wrote
     */
    public record PersonCard(Long id, String name, String background, List<String> interests, long projectCount,
                             long ideaCount) {
    }

    /**
     * One person's page.
     *
     * @param profile their profile
     * @param projects the projects they own or are a member of, most recently changed first
     * @param ideas the ideas they wrote, newest first
     */
    public record PersonPage(ProfileView profile, List<ProjectSummary> projects, List<IdeaSummary> ideas) {
    }

    private static final String DIRECTORY_SQL = """
            SELECT u.id, coalesce(u.name, u.email) AS name, u.background, u.interests,
                   (SELECT count(*) FROM project p
                    WHERE p.owner_id = u.id
                       OR EXISTS (SELECT 1 FROM project_member m WHERE m.project_id = p.id AND m.user_id = u.id))
                       AS project_count,
                   (SELECT count(*) FROM idea i WHERE i.author_id = u.id) AS idea_count
            FROM app_user u
            WHERE u.allowlisted
            ORDER BY u.name, u.email
            """;

    private final JdbcClient jdbc;
    private final AppUserRepository users;
    private final IdeaRepository ideas;
    private final ProjectService projectService;

    /**
     * Creates the service.
     *
     * @param jdbc runs the list query
     * @param users where users are stored
     * @param ideas where ideas are stored
     * @param projectService finds a person's projects
     */
    public DirectoryService(JdbcClient jdbc, AppUserRepository users, IdeaRepository ideas,
                            ProjectService projectService) {
        this.jdbc = jdbc;
        this.users = users;
        this.ideas = ideas;
        this.projectService = projectService;
    }

    /**
     * Lists everyone who can sign in, by name, with how much they have going on.
     *
     * @return the lab's members
     */
    public List<PersonCard> directory() {
        return jdbc.sql(DIRECTORY_SQL)
                .query((rs, row) -> new PersonCard(rs.getLong("id"), rs.getString("name"), rs.getString("background"),
                        List.of((String[]) rs.getArray("interests").getArray()), rs.getLong("project_count"),
                        rs.getLong("idea_count")))
                .list();
    }

    /**
     * Returns one person's profile, projects and ideas.
     *
     * @param id the person's id
     * @return their page
     * @throws ResponseStatusException {@code 404} if there is no such person, or they can't sign in
     */
    public PersonPage person(Long id) {
        AppUser user = users.findById(id)
                .filter(AppUser::isAllowlisted)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No such person"));
        return new PersonPage(ProfileView.of(user), projectService.involving(user),
                ideas.findByAuthorIdOrderByCreatedAtDescIdDesc(id).stream().map(IdeaSummary::of).toList());
    }
}

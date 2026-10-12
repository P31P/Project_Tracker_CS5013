package tracker.projects;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tracker.data.AppUserRepository;
import tracker.projects.ProjectDtos.Person;

/**
 * Lists everyone who can sign in, so they can be added to a project.
 */
@RestController
public class PeopleController {

    private final AppUserRepository users;

    /**
     * Creates the controller.
     *
     * @param users where users are stored
     */
    public PeopleController(AppUserRepository users) {
        this.users = users;
    }

    /**
     * Lists everyone who may sign in, by name. Handles {@code GET /api/people}.
     *
     * @return the allowlisted people
     */
    @GetMapping("/api/people")
    public List<Person> people() {
        return ProjectService.people(users.findByAllowlistedTrueOrderByNameAscEmailAsc());
    }
}

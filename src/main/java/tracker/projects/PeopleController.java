package tracker.projects;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tracker.data.AppUserRepository;
import tracker.projects.ProjectDtos.Person;

/** Everyone who can sign in, so they can be added to a project. */
@RestController
public class PeopleController {

    private final AppUserRepository users;

    public PeopleController(AppUserRepository users) {
        this.users = users;
    }

    @GetMapping("/api/people")
    public List<Person> people() {
        return ProjectService.people(users.findByAllowlistedTrueOrderByNameAscEmailAsc());
    }
}

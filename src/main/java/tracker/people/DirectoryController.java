package tracker.people;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tracker.people.DirectoryService.PersonCard;
import tracker.people.DirectoryService.PersonPage;

/**
 * The People tab's API. {@code GET /api/people} itself is the short list used to pick project members, in
 * {@code tracker.projects.PeopleController}.
 */
@RestController
@RequestMapping("/api/people")
public class DirectoryController {

    private final DirectoryService directory;

    /**
     * Creates the controller.
     *
     * @param directory finds the lab's members
     */
    public DirectoryController(DirectoryService directory) {
        this.directory = directory;
    }

    /**
     * Lists everyone who can sign in, with their background, interests and counts. Handles
     * {@code GET /api/people/directory}.
     *
     * @return the lab's members, by name
     */
    @GetMapping("/directory")
    public List<PersonCard> directory() {
        return directory.directory();
    }

    /**
     * Returns one person's profile, projects and ideas. Handles {@code GET /api/people/{id}}.
     *
     * @param id the person's id
     * @return their page
     */
    @GetMapping("/{id}")
    public PersonPage person(@PathVariable Long id) {
        return directory.person(id);
    }
}

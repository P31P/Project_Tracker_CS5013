package tracker.auth;

import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tracker.data.AppUser;
import tracker.data.AppUserRepository;
import tracker.data.Role;

/**
 * Gives and removes access, and changes roles. Only a Lab Manager or an admin may do this. Two rules keep the lab
 * from locking itself out: nobody changes their own access, and only an admin can make or change an admin, so an
 * admin can only be removed by another admin.
 */
@Service
@Transactional
public class AccessService {

    /**
     * One person on the Manage access page.
     *
     * @param id the person's id
     * @param name their name, or {@code null} if they haven't signed in yet and none was given
     * @param email their email address
     * @param role what they may do
     * @param allowlisted whether they can sign in
     */
    public record PersonAccess(Long id, String name, String email, Role role, boolean allowlisted) {
    }

    /**
     * Someone to give access to.
     *
     * @param email their Google account's email address; required
     * @param name their name; optional
     * @param role their role; {@code MEMBER} if left out
     */
    public record NewPerson(String email, String name, Role role) {
    }

    /**
     * A change to someone's access. Fields left out ({@code null}) are not changed.
     *
     * @param role the new role
     * @param allowlisted {@code false} removes their access, {@code true} restores it
     */
    public record AccessChange(Role role, Boolean allowlisted) {
    }

    private final AppUserRepository users;

    /**
     * Creates the service.
     *
     * @param users where users are stored
     */
    public AccessService(AppUserRepository users) {
        this.users = users;
    }

    /**
     * Lists everyone who has ever been given access, by name.
     *
     * @param actor the signed-in user
     * @return everyone, with their role and whether they can sign in
     * @throws ResponseStatusException {@code 403} if the actor isn't a Lab Manager or an admin
     */
    @Transactional(readOnly = true)
    public List<PersonAccess> list(AppUser actor) {
        requireManager(actor);
        return users.findAllByOrderByNameAscEmailAsc().stream().map(AccessService::view).toList();
    }

    /**
     * Lets someone sign in. A new email gets a new row; an email already known has its access restored.
     *
     * @param actor the signed-in user
     * @param person who to add
     * @return the person's access
     * @throws ResponseStatusException {@code 403} if the actor may not do this, {@code 400} if the email is missing
     *         or doesn't look like one
     */
    public PersonAccess add(AppUser actor, NewPerson person) {
        requireManager(actor);
        String email = person.email() == null ? "" : person.email().strip().toLowerCase(Locale.ROOT);
        if (!email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Give the Google account's email address");
        }
        Role role = person.role() == null ? Role.MEMBER : person.role();
        requireAllowedToGive(actor, role);
        String name = person.name() == null || person.name().isBlank() ? null : person.name().strip();
        AppUser user = users.findByEmail(email).orElseGet(() -> new AppUser(email, name));
        if (user.getRole() == Role.ADMIN && role != Role.ADMIN) {
            requireAdmin(actor);
        }
        user.setAllowlisted(true);
        user.setRole(role);
        return view(users.save(user));
    }

    /**
     * Changes someone's role, or removes or restores their access. Removing never deletes them, so their projects,
     * ideas and comments keep their name.
     *
     * @param actor the signed-in user
     * @param id the person's id
     * @param change what to change
     * @return the person's access after the change
     * @throws ResponseStatusException {@code 403} if the actor may not make this change, {@code 404} if there is no
     *         such person, or {@code 400} if it would change the actor's own access
     */
    public PersonAccess change(AppUser actor, Long id, AccessChange change) {
        requireManager(actor);
        AppUser user = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No such person"));
        if (user.getId().equals(actor.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You can't change your own access");
        }
        Role role = change.role() == null ? user.getRole() : change.role();
        boolean allowlisted = change.allowlisted() == null ? user.isAllowlisted() : change.allowlisted();
        if (user.getRole() == Role.ADMIN || role == Role.ADMIN) {
            requireAdmin(actor);
        }
        user.setRole(role);
        user.setAllowlisted(allowlisted);
        return view(user);
    }

    private static void requireManager(AppUser actor) {
        if (actor == null || !actor.getRole().managesAccess()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only a Lab Manager can manage access");
        }
    }

    private static void requireAdmin(AppUser actor) {
        if (actor.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only an admin can make or change an admin");
        }
    }

    private static void requireAllowedToGive(AppUser actor, Role role) {
        if (role == Role.ADMIN) {
            requireAdmin(actor);
        }
    }

    private static PersonAccess view(AppUser user) {
        return new PersonAccess(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.isAllowlisted());
    }
}

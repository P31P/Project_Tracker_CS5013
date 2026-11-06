package tracker.auth;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tracker.auth.AccessService.AccessChange;
import tracker.auth.AccessService.NewPerson;
import tracker.auth.AccessService.PersonAccess;

/** The Manage access API under {@code /api/access}, for Lab Managers and admins only. */
@RestController
@RequestMapping("/api/access")
public class AccessController {

    private final AccessService access;
    private final CurrentUser currentUser;

    /**
     * Creates the controller.
     *
     * @param access does the work and checks who may do it
     * @param currentUser finds the signed-in user
     */
    public AccessController(AccessService access, CurrentUser currentUser) {
        this.access = access;
        this.currentUser = currentUser;
    }

    /**
     * Lists everyone who has ever been given access. Handles {@code GET /api/access}.
     *
     * @param user the signed-in user
     * @return everyone, with their role and whether they can sign in
     */
    @GetMapping
    public List<PersonAccess> list(@AuthenticationPrincipal OidcUser user) {
        return access.list(currentUser.of(user));
    }

    /**
     * Lets someone sign in. Handles {@code POST /api/access} and answers {@code 201}.
     *
     * @param person who to add
     * @param user the signed-in user
     * @return the person's access
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PersonAccess add(@RequestBody NewPerson person, @AuthenticationPrincipal OidcUser user) {
        return access.add(currentUser.of(user), person);
    }

    /**
     * Changes someone's role, or removes or restores their access. Handles {@code PATCH /api/access/{id}}.
     *
     * @param id the person's id
     * @param change what to change
     * @param user the signed-in user
     * @return the person's access after the change
     */
    @PatchMapping("/{id}")
    public PersonAccess change(@PathVariable Long id, @RequestBody AccessChange change,
                               @AuthenticationPrincipal OidcUser user) {
        return access.change(currentUser.of(user), id, change);
    }
}

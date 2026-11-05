package tracker.profile;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tracker.data.AppUser;
import tracker.data.AppUserRepository;
import tracker.projects.ProjectService;

/** Reads and saves people's own profiles. */
@Service
@Transactional
public class ProfileService {

    static final int MAX_BIO = 2000;
    static final int MAX_BACKGROUND = 200;
    static final int MAX_INTERESTS = 20;

    private final AppUserRepository users;

    /**
     * Creates the service.
     *
     * @param users where users and their profiles are stored
     */
    public ProfileService(AppUserRepository users) {
        this.users = users;
    }

    /**
     * Returns a person's own profile.
     *
     * @param user the signed-in user; may be {@code null}
     * @return their profile
     * @throws ResponseStatusException {@code 404} if there is no stored user
     */
    @Transactional(readOnly = true)
    public ProfileView mine(AppUser user) {
        return ProfileView.of(stored(user));
    }

    /**
     * Replaces a person's introduction, background and interests.
     *
     * @param user the signed-in user; may be {@code null}
     * @param edit the new profile
     * @return the saved profile
     * @throws ResponseStatusException {@code 404} if there is no stored user, {@code 400} if a field is too long
     *         or there are too many interests
     */
    public ProfileView update(AppUser user, ProfileEdit edit) {
        AppUser stored = stored(user);
        List<String> interests = ProjectService.tags(edit.interests());
        if (interests.size() > MAX_INTERESTS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pick at most " + MAX_INTERESTS + " interests");
        }
        stored.setBio(limited(edit.bio(), MAX_BIO, "introduction"));
        stored.setBackground(limited(edit.background(), MAX_BACKGROUND, "background"));
        stored.setInterests(interests);
        return ProfileView.of(users.save(stored));
    }

    // the user given was loaded outside this transaction, so load it again to save changes
    private AppUser stored(AppUser user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No profile for this account");
        }
        return users.findById(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No profile for this account"));
    }

    private static String limited(String text, int max, String what) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String trimmed = text.strip();
        if (trimmed.length() > max) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The " + what + " can be at most " + max + " characters");
        }
        return trimmed;
    }
}

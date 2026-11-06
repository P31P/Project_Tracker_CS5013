package tracker.profile;

import java.util.List;
import tracker.auth.CurrentUser;
import tracker.data.AppUser;

/**
 * A lab member's profile, as shown on their Home page and in the People tab.
 *
 * @param id the person's id
 * @param name their name, or their email if no name is known
 * @param email their email address, so others can get in touch
 * @param bio their introduction in Markdown, or {@code null}
 * @param background their study background, or {@code null}
 * @param interests their fields of interest, as tags
 */
public record ProfileView(Long id, String name, String email, String bio, String background, List<String> interests) {

    /**
     * Builds the profile of a user.
     *
     * @param user the user
     * @return their profile
     */
    public static ProfileView of(AppUser user) {
        return new ProfileView(user.getId(), CurrentUser.nameOf(user), user.getEmail(), user.getBio(),
                user.getBackground(), user.getInterests());
    }
}

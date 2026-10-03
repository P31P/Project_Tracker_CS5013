package tracker.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import tracker.data.AppUser;
import tracker.data.AppUserRepository;
import tracker.support.PostgresTestBase;

@Transactional
class AllowlistServiceTest extends PostgresTestBase {

    @Autowired
    private AllowlistService allowlist;

    @Autowired
    private AppUserRepository users;

    @Test
    void allowlistedUserIsAllowed() {
        saveUser("member@example.com", true);

        assertThat(allowlist.isAllowed("member@example.com")).isTrue();
    }

    @Test
    void userNotAllowlistedIsRefused() {
        saveUser("pending@example.com", false);

        assertThat(allowlist.isAllowed("pending@example.com")).isFalse();
    }

    @Test
    void unknownEmailIsRefused() {
        assertThat(allowlist.isAllowed("stranger@example.com")).isFalse();
    }

    @Test
    void emailCaseDoesNotMatter() {
        saveUser("member@example.com", true);

        assertThat(allowlist.isAllowed("MEMBER@Example.com")).isTrue();
    }

    @Test
    void missingEmailIsRefused() {
        assertThat(allowlist.isAllowed(null)).isFalse();
    }

    private void saveUser(String email, boolean allowlisted) {
        AppUser user = new AppUser(email, "Test User");
        user.setAllowlisted(allowlisted);
        users.saveAndFlush(user);
    }
}

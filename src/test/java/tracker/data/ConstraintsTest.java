package tracker.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import tracker.support.PostgresTestBase;

@Transactional
class ConstraintsTest extends PostgresTestBase {

    @Autowired
    private ProjectRepository projects;

    @Autowired
    private AppUserRepository users;

    @Test
    void projectSavesWithOnlyATitle() {
        Project saved = projects.saveAndFlush(new Project("Protein folding survey"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void blankTitleIsRejected() {
        assertThatThrownBy(() -> projects.saveAndFlush(new Project("   ")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void duplicateEmailIsRejected() {
        users.saveAndFlush(new AppUser("member@example.com", "First"));

        assertThatThrownBy(() -> users.saveAndFlush(new AppUser("member@example.com", "Second")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}

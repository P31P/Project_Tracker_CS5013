package tracker.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import tracker.support.PostgresTestBase;

@Transactional
class SlackTablesTest extends PostgresTestBase {

    @Autowired
    private SlackMessageRepository messages;

    @Autowired
    private PaperRepository papers;

    @Autowired
    private PaperShareRepository shares;

    @Test
    void duplicateSlackMessageIsRejected() {
        messages.saveAndFlush(new SlackMessage("C123", "1696412345.000100", "U123", "@helper first"));

        assertThatThrownBy(() -> messages.saveAndFlush(
                new SlackMessage("C123", "1696412345.000100", "U123", "@helper again")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void duplicatePaperUrlIsRejected() {
        papers.saveAndFlush(new Paper("https://arxiv.org/abs/1706.03762"));

        assertThatThrownBy(() -> papers.saveAndFlush(new Paper("https://arxiv.org/abs/1706.03762")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void duplicateShareIsRejected() {
        Paper paper = papers.saveAndFlush(new Paper("https://doi.org/10.1007/s11263-015-0816-y"));
        SlackMessage message = messages.saveAndFlush(
                new SlackMessage("C123", "1696412345.000200", "U123", "@helper <link>"));
        shares.saveAndFlush(new PaperShare(paper, message, "https://link.springer.com/article/10.1007/s11263-015-0816-y"));

        assertThatThrownBy(() -> shares.saveAndFlush(
                new PaperShare(paper, message, "https://doi.org/10.1007/s11263-015-0816-y")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void newPaperStartsPending() {
        Paper saved = papers.saveAndFlush(new Paper("https://arxiv.org/abs/2401.12345"));

        assertThat(saved.getStatus()).isEqualTo(PaperStatus.PENDING);
        assertThat(saved.getCreatedAt()).isNotNull();
    }
}

package tracker.slack;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class LinkExtractorTest {

    private final LinkExtractor extractor = new LinkExtractor();

    private List<String> normalized(String text) {
        return extractor.extract(text).stream().map(PaperLink::normalizedUrl).toList();
    }

    @Test
    void slackLinkWithLabel() {
        List<PaperLink> links = extractor.extract("read <https://arxiv.org/abs/1706.03762|Attention Is All You Need> today");

        assertThat(links).containsExactly(new PaperLink("https://arxiv.org/abs/1706.03762", "https://arxiv.org/abs/1706.03762"));
    }

    @Test
    void slackLinkWithoutLabel() {
        assertThat(normalized("<https://arxiv.org/abs/1706.03762>")).containsExactly("https://arxiv.org/abs/1706.03762");
    }

    @Test
    void bareLinkLosesTrailingPunctuation() {
        assertThat(normalized("see https://doi.org/10.1145/3292500.3330701.")).containsExactly("https://doi.org/10.1145/3292500.3330701");
    }

    @Test
    void arxivAbsAndPdfWithAndWithoutVersionAreTheSamePaper() {
        String text = """
                https://arxiv.org/abs/2401.12345v2
                http://arxiv.org/pdf/2401.12345v3.pdf
                https://arxiv.org/pdf/2401.12345
                """;

        assertThat(normalized(text)).containsExactly("https://arxiv.org/abs/2401.12345");
    }

    @Test
    void oldStyleArxivId() {
        assertThat(normalized("https://arxiv.org/abs/hep-th/9901001v1")).containsExactly("https://arxiv.org/abs/hep-th/9901001");
    }

    @Test
    void arxivListingPagesAreNotPapers() {
        assertThat(extractor.extract("https://arxiv.org/list/cs.LG/recent")).isEmpty();
    }

    @Test
    void doiOrgLowercasesTheDoi() {
        assertThat(normalized("https://dx.doi.org/10.1000/ABC.123")).containsExactly("https://doi.org/10.1000/abc.123");
    }

    @Test
    void doiInsidePublisherUrls() {
        String text = """
                https://link.springer.com/article/10.1007/s11263-015-0816-y
                https://dl.acm.org/doi/10.1145/3292500.3330701
                https://onlinelibrary.wiley.com/doi/full/10.1002/anie.202100001
                https://link.springer.com/content/pdf/10.1007/s11263-015-0816-y.pdf
                """;

        assertThat(normalized(text)).containsExactly(
                "https://doi.org/10.1007/s11263-015-0816-y",
                "https://doi.org/10.1145/3292500.3330701",
                "https://doi.org/10.1002/anie.202100001");
    }

    @Test
    void natureArticleBecomesItsDoi() {
        assertThat(normalized("https://www.nature.com/articles/s41593-026-02402-0")).containsExactly("https://doi.org/10.1038/s41593-026-02402-0");
        assertThat(normalized("https://www.nature.com/articles/s41593-026-02402-0.pdf?utm_source=x")).containsExactly("https://doi.org/10.1038/s41593-026-02402-0");
    }

    @Test
    void mentionWithSurroundingText() {
        String text = "<@U0123ABCD> found this research paper helpful : <https://www.nature.com/articles/s41593-026-02402-0>";

        assertThat(extractor.extract(text)).containsExactly(new PaperLink(
                "https://www.nature.com/articles/s41593-026-02402-0", "https://doi.org/10.1038/s41593-026-02402-0"));
    }

    @Test
    void ieeeDocumentUrls() {
        assertThat(normalized("https://ieeexplore.ieee.org/document/9156559/?utm_source=x")).containsExactly("https://ieeexplore.ieee.org/document/9156559");
        assertThat(normalized("https://ieeexplore.ieee.org/abstract/document/9156559")).containsExactly("https://ieeexplore.ieee.org/document/9156559");
    }

    @Test
    void plainPdfLinkKeepsItsPathAndDropsTracking() {
        assertThat(normalized("HTTP://Example.ORG/Papers/Draft.pdf?utm_source=slack&page=2&fbclid=abc"))
                .containsExactly("https://example.org/Papers/Draft.pdf?page=2");
    }

    @Test
    void slackEscapedAmpersandIsDecoded() {
        assertThat(normalized("<https://example.org/a.pdf?id=1&amp;page=2|paper>")).containsExactly("https://example.org/a.pdf?id=1&page=2");
    }

    @Test
    void nonPaperLinksAreIgnored() {
        String text = """
                <https://github.com/ramanlab/tracker|repo>
                https://www.youtube.com/watch?v=abc123
                https://docs.google.com/document/d/abc/edit
                https://arxiv.org/
                <mailto:someone@example.org|someone>
                """;

        assertThat(extractor.extract(text)).isEmpty();
    }

    @Test
    void messageWithoutLinksOrNullGivesNothing() {
        assertThat(extractor.extract("lunch at 1?")).isEmpty();
        assertThat(extractor.extract("")).isEmpty();
        assertThat(extractor.extract(null)).isEmpty();
    }

    @Test
    void duplicatesInOneMessageAreReturnedOnce() {
        assertThat(extractor.extract("<https://arxiv.org/abs/1706.03762|https://arxiv.org/abs/1706.03762> again https://arxiv.org/pdf/1706.03762v5"))
                .hasSize(1);
    }

    @Test
    void mixedMessageKeepsOnlyPapersInOrder() {
        String text = "<@U123> new ones: <https://github.com/a/b|code> <https://arxiv.org/abs/2401.00001|A> and https://doi.org/10.1000/xyz";

        assertThat(normalized(text)).containsExactly("https://arxiv.org/abs/2401.00001", "https://doi.org/10.1000/xyz");
    }
}

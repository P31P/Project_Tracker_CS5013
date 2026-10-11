package tracker.papers;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tracker.data.Paper;
import tracker.data.PaperRepository;
import tracker.data.PaperSource;
import tracker.data.PaperStatus;
import tracker.support.PostgresTestBase;

@AutoConfigureMockMvc
class PaperControllerTest extends PostgresTestBase {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PaperRepository papers;

    @Test
    void listReturnsAllPapers() throws Exception {
        Paper paper = makePaper(1L, "https://arxiv.org/abs/1706.03762", PaperStatus.PENDING);
        when(papers.findAll()).thenReturn(List.of(paper));

        mvc.perform(get("/api/papers").with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].normalizedUrl").value("https://arxiv.org/abs/1706.03762"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void resolvedPaperShowsItsDetails() throws Exception {
        Paper paper = makePaper(4L, "https://arxiv.org/abs/1706.03762", PaperStatus.PENDING);
        paper.markResolved("Attention Is All You Need", List.of("Ashish Vaswani", "Noam Shazeer"),
                LocalDate.of(2017, 6, 12), null, PaperSource.REGISTRY);
        when(papers.findById(4L)).thenReturn(Optional.of(paper));

        mvc.perform(get("/api/papers/4").with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.title").value("Attention Is All You Need"))
                .andExpect(jsonPath("$.authors[1]").value("Noam Shazeer"))
                .andExpect(jsonPath("$.publishedOn").value("2017-06-12"))
                .andExpect(jsonPath("$.source").value("REGISTRY"));
    }

    @Test
    void listFiltersByStatus() throws Exception {
        Paper paper = makePaper(2L, "https://doi.org/10.1000/xyz", PaperStatus.RESOLVED);
        when(papers.findByStatus(PaperStatus.RESOLVED)).thenReturn(List.of(paper));

        mvc.perform(get("/api/papers?status=RESOLVED").with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("RESOLVED"));
    }

    @Test
    void getByIdReturnsOnePaper() throws Exception {
        Paper paper = makePaper(3L, "https://arxiv.org/abs/2401.12345", PaperStatus.PENDING);
        when(papers.findById(3L)).thenReturn(Optional.of(paper));

        mvc.perform(get("/api/papers/3").with(oidcLogin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3));
    }

    @Test
    void getByIdReturns404WhenNotFound() throws Exception {
        when(papers.findById(99L)).thenReturn(Optional.empty());

        mvc.perform(get("/api/papers/99").with(oidcLogin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticatedRequestIsRedirected() throws Exception {
        mvc.perform(get("/api/papers"))
                .andExpect(status().is3xxRedirection());
    }

    private static Paper makePaper(Long id, String url, PaperStatus status) {
        Paper p = new Paper(url);
        // set id via reflection since the constructor doesn't accept one
        try {
            var field = Paper.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(p, id);
            var createdAt = Paper.class.getDeclaredField("createdAt");
            createdAt.setAccessible(true);
            createdAt.set(p, OffsetDateTime.now());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        p.setStatus(status);
        return p;
    }
}

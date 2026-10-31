package tracker.search;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The search API under {@code /api/search}. */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService search;

    /**
     * Creates the controller.
     *
     * @param search runs the searches
     */
    public SearchController(SearchService search) {
        this.search = search;
    }

    /**
     * Searches projects, ideas and papers. Handles {@code GET /api/search}.
     *
     * @param q the text to look for
     * @param type only {@code PROJECT}, {@code IDEA} or {@code PAPER} results; all if left out
     * @return the best matches, best first
     */
    @GetMapping
    public List<SearchResult> search(@RequestParam(defaultValue = "") String q,
                                     @RequestParam(required = false) SearchType type) {
        return search.search(q, type);
    }
}

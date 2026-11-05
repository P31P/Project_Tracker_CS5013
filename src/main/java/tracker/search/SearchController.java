package tracker.search;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tracker.projects.ProjectService;

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
     * Searches projects, ideas and papers. Handles {@code GET /api/search}. Every parameter is optional; without
     * {@code q}, the filters alone list what matches, newest first.
     *
     * @param q the text to look for
     * @param type only {@code PROJECT}, {@code IDEA} or {@code PAPER} results; all if left out
     * @param tag tags every result must have; repeat the parameter for more than one
     * @param person the id of a person whose projects and ideas to keep
     * @param from the first day things were added, as {@code 2026-11-05}
     * @param to the last day things were added, as {@code 2026-11-05}
     * @return the best matches, best first
     * @throws ResponseStatusException {@code 400} if a date can't be read or {@code from} is after {@code to}
     */
    @GetMapping
    public List<SearchResult> search(@RequestParam(defaultValue = "") String q,
                                     @RequestParam(required = false) SearchType type,
                                     @RequestParam(required = false) List<String> tag,
                                     @RequestParam(required = false) Long person,
                                     @RequestParam(required = false) String from,
                                     @RequestParam(required = false) String to) {
        LocalDate first = date(from);
        LocalDate last = date(to);
        if (first != null && last != null && first.isAfter(last)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The start date is after the end date");
        }
        return search.search(q, new SearchFilters(type, ProjectService.tags(tag), person, first, last));
    }

    private static LocalDate date(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text.strip());
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dates look like 2026-11-05, not " + text);
        }
    }
}

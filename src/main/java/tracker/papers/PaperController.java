package tracker.papers;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tracker.data.PaperRepository;
import tracker.data.PaperStatus;

@RestController
@RequestMapping("/api/papers")
public class PaperController {

    private final PaperRepository papers;

    public PaperController(PaperRepository papers) {
        this.papers = papers;
    }

    @GetMapping
    public List<PaperDto> list(@RequestParam(required = false) PaperStatus status) {
        var result = status != null ? papers.findByStatus(status) : papers.findAll();
        return result.stream().map(PaperDto::from).toList();
    }

    @GetMapping("/{id}")
    public PaperDto get(@PathVariable Long id) {
        return papers.findById(id)
                .map(PaperDto::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}

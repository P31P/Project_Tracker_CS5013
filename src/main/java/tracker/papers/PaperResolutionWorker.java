package tracker.papers;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@ConditionalOnProperty(name = "papers.resolution-enabled", havingValue = "true")
public class PaperResolutionWorker {

    private static final int PAPERS_PER_RUN = 10;

    private final PaperResolutionService resolution;

    public PaperResolutionWorker(PaperResolutionService resolution) {
        this.resolution = resolution;
    }

    @Scheduled(fixedDelayString = "${papers.resolution-interval-ms:30000}")
    public void resolvePending() {
        for (int i = 0; i < PAPERS_PER_RUN && resolution.resolveNext(); i++) {
            // each call resolves one paper
        }
    }
}

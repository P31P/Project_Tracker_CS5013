package tracker.papers;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Resolves pending papers on a schedule. Runs only when {@code papers.resolution-enabled} is {@code true}. */
@Component
@EnableScheduling
@ConditionalOnProperty(name = "papers.resolution-enabled", havingValue = "true")
public class PaperResolutionWorker {

    private static final int PAPERS_PER_RUN = 10;

    private final PaperResolutionService resolution;

    /**
     * Creates the worker.
     *
     * @param resolution resolves one paper at a time
     */
    public PaperResolutionWorker(PaperResolutionService resolution) {
        this.resolution = resolution;
    }

    /**
     * Resolves up to ten due papers. Runs every {@code papers.resolution-interval-ms} (30 seconds by default) after the
     * previous run finishes.
     */
    @Scheduled(fixedDelayString = "${papers.resolution-interval-ms:30000}")
    public void resolvePending() {
        for (int i = 0; i < PAPERS_PER_RUN && resolution.resolveNext(); i++) {
            // each call resolves one paper
        }
    }
}

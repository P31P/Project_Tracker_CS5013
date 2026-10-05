package tracker.papers;

import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tracker.support.PostgresTestBase;

@SpringBootTest(properties = {"papers.resolution-enabled=true", "papers.resolution-interval-ms=100"})
class PaperResolutionWorkerTest extends PostgresTestBase {

    @MockitoBean
    private PaperResolutionService resolution;

    @Test
    void workerCallsTheServiceOnASchedule() {
        verify(resolution, timeout(5000).atLeastOnce()).resolveNext();
    }
}

package tracker.projects;

import java.time.OffsetDateTime;
import java.util.List;
import tracker.data.ProjectStatus;
import tracker.data.UpdateKind;

/** What the projects and ideas API sends and receives. */
public final class ProjectDtos {

    private ProjectDtos() {
    }

    public record ProjectRequest(String title, String description, ProjectStatus status) {
    }

    public record UpdateRequest(String body) {
    }

    public record IdeaRequest(String title, String description) {
    }

    public record ProjectSummary(Long id, String title, String description, ProjectStatus status, String ownerName,
                                 OffsetDateTime createdAt, OffsetDateTime updatedAt, long updateCount) {
    }

    public record ProjectDetail(Long id, String title, String description, ProjectStatus status, String ownerName,
                                OffsetDateTime createdAt, OffsetDateTime updatedAt, List<TimelineEntry> timeline) {
    }

    public record TimelineEntry(Long id, UpdateKind kind, String body, String authorName, OffsetDateTime createdAt) {
    }

    public record IdeaView(Long id, String title, String description, String authorName, OffsetDateTime createdAt,
                           Long projectId, String projectTitle) {
    }
}

package tracker.projects;

import java.time.OffsetDateTime;
import java.util.List;
import tracker.data.ProjectStatus;
import tracker.data.UpdateKind;

/** What the projects and ideas API sends and receives. */
public final class ProjectDtos {

    private ProjectDtos() {
    }

    // fields left out (null) are not changed by a PATCH
    public record ProjectRequest(String title, String description, ProjectStatus status, List<String> tags,
                                 List<Long> memberIds) {
    }

    public record UpdateRequest(String body) {
    }

    public record LinkRequest(String label, String url) {
    }

    public record IdeaRequest(String title, String description) {
    }

    public record Person(Long id, String name) {
    }

    public record LinkView(Long id, String label, String url) {
    }

    public record ProjectSummary(Long id, String title, String description, ProjectStatus status, String ownerName,
                                 List<String> tags, List<Person> members, OffsetDateTime createdAt,
                                 OffsetDateTime updatedAt, long updateCount) {
    }

    public record ProjectDetail(Long id, String title, String description, ProjectStatus status, String ownerName,
                                List<String> tags, List<Person> members, List<LinkView> links,
                                OffsetDateTime createdAt, OffsetDateTime updatedAt, List<TimelineEntry> timeline) {
    }

    public record TimelineEntry(Long id, UpdateKind kind, String body, String authorName, OffsetDateTime createdAt) {
    }

    public record IdeaView(Long id, String title, String description, String authorName, OffsetDateTime createdAt,
                           Long projectId, String projectTitle) {
    }
}

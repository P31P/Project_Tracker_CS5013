package tracker.projects;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import tracker.data.ProjectStatus;
import tracker.data.UpdateKind;

/**
 * What the projects and ideas API sends and receives, as JSON.
 */
public final class ProjectDtos {

    private ProjectDtos() {
    }

    /**
     * A project to create, or the changes to make to one. In a {@code PATCH}, fields left out ({@code null}) are not
     * changed.
     *
     * @param title the title; required when creating
     * @param description the description; blank removes it
     * @param status the status; {@code ACTIVE} if left out when creating
     * @param tags the tags, tidied by the server
     * @param memberIds the ids of the members; when changing, replaces the current members
     * @param startedOn the start date; today if left out when creating
     * @param details the longer write-up; when changing, replaces all five parts
     * @param openForCollab whether others can ask to join; {@code false} if left out when creating
     */
    public record ProjectRequest(String title, String description, ProjectStatus status, List<String> tags,
                                 List<Long> memberIds, LocalDate startedOn, Details details, Boolean openForCollab) {
    }

    /**
     * The longer write-up of a project or an idea. Every part is optional Markdown; a blank part is stored as none.
     *
     * @param coreQuestion the question it tries to answer
     * @param motivation why it matters
     * @param approach how it could be done
     * @param resources the data and resources it needs
     * @param challenges what could make it hard
     */
    public record Details(String coreQuestion, String motivation, String approach, String resources,
                          String challenges) {
    }

    /**
     * A note to add to a project's timeline.
     *
     * @param body the note; must not be blank
     */
    public record UpdateRequest(String body) {
    }

    /**
     * A link to add to a project.
     *
     * @param label the text to show; if blank, the site's name is used
     * @param url the address; must start with {@code http://} or {@code https://}
     */
    public record LinkRequest(String label, String url) {
    }

    /**
     * An idea to save.
     *
     * @param title the idea; must not be blank
     * @param description more detail; optional
     * @param tags the tags, tidied by the server; optional
     * @param details the longer write-up; optional
     * @param openForCollab whether the author is looking for people to work on it with; optional
     */
    public record IdeaRequest(String title, String description, List<String> tags, Details details,
                              Boolean openForCollab) {
    }

    /**
     * Someone who can sign in, as shown on a project.
     *
     * @param id the user's id
     * @param name the user's name, or their email if the name is unknown
     */
    public record Person(Long id, String name) {
    }

    /**
     * A paper from the archive to list as a reference.
     *
     * @param paperId the paper's id
     */
    public record PaperRequest(Long paperId) {
    }

    /**
     * A paper from the archive listed as a reference.
     *
     * @param id the paper's id
     * @param title its title, or its address until the details are found
     * @param url its address
     */
    public record PaperRef(Long id, String title, String url) {
    }

    /**
     * A link on a project or an idea.
     *
     * @param id the link's id
     * @param label the text to show
     * @param url the address
     */
    public record LinkView(Long id, String label, String url) {
    }

    /**
     * A project as shown in the list and on the board.
     *
     * @param id the project's id
     * @param title the title
     * @param description the description, or {@code null}
     * @param status the status
     * @param ownerName who started it, or {@code null}
     * @param tags the tags
     * @param members the members, by name
     * @param createdAt when it was created
     * @param updatedAt when it last changed
     * @param updateCount how many entries its timeline has
     * @param startedOn the start date
     * @param openForCollab whether others can ask to join
     * @param voteCount how many people upvoted it
     */
    public record ProjectSummary(Long id, String title, String description, ProjectStatus status, String ownerName,
                                 List<String> tags, List<Person> members, OffsetDateTime createdAt,
                                 OffsetDateTime updatedAt, long updateCount, LocalDate startedOn,
                                 boolean openForCollab, long voteCount) {
    }

    /**
     * A project with everything on its page.
     *
     * @param id the project's id
     * @param title the title
     * @param description the description, or {@code null}
     * @param status the status
     * @param ownerName who started it, or {@code null}
     * @param tags the tags
     * @param members the members, by name
     * @param links the links, in the order they were added
     * @param createdAt when it was created
     * @param updatedAt when it last changed
     * @param timeline the timeline, newest first
     * @param startedOn the start date
     * @param details the longer write-up
     * @param openForCollab whether others can ask to join
     * @param collabOpenedAt when it was opened for collaboration, or {@code null} if it isn't open
     * @param papers papers from the archive listed as references, by title
     * @param voteCount how many people upvoted it
     * @param votedByMe whether the signed-in user upvoted it
     * @param commentCount how many comments its discussion has
     * @param ownerId the id of who started it, or {@code null}
     */
    public record ProjectDetail(Long id, String title, String description, ProjectStatus status, String ownerName,
                                List<String> tags, List<Person> members, List<LinkView> links,
                                OffsetDateTime createdAt, OffsetDateTime updatedAt, List<TimelineEntry> timeline,
                                LocalDate startedOn, Details details, boolean openForCollab,
                                OffsetDateTime collabOpenedAt, List<PaperRef> papers, long voteCount,
                                boolean votedByMe, long commentCount, Long ownerId) {
    }

    /**
     * One entry on a project's timeline.
     *
     * @param id the entry's id
     * @param kind a note or a status change
     * @param body the text
     * @param authorName who wrote it, or {@code null}
     * @param createdAt when it was written
     */
    public record TimelineEntry(Long id, UpdateKind kind, String body, String authorName, OffsetDateTime createdAt) {
    }

    /**
     * An idea as shown on the Ideas tab.
     *
     * @param id the idea's id
     * @param title the title
     * @param description the description, or {@code null}
     * @param authorName who wrote it, or {@code null}
     * @param tags the tags
     * @param createdAt when it was written
     * @param projectId the project it became, or {@code null} if it is still open
     * @param projectTitle that project's title, or {@code null}
     * @param voteCount how many people voted for it
     * @param votedByMe whether the signed-in user voted for it
     * @param commentCount how many comments its discussion has
     * @param details the longer write-up
     * @param openForCollab whether the author is looking for people to work on it with
     * @param links web links listed as references, in the order they were added
     * @param papers papers from the archive listed as references, by title
     * @param authorId the id of who wrote it, or {@code null}
     */
    public record IdeaView(Long id, String title, String description, String authorName, List<String> tags,
                           OffsetDateTime createdAt, Long projectId, String projectTitle, int voteCount,
                           boolean votedByMe, long commentCount, Details details, boolean openForCollab,
                           List<LinkView> links, List<PaperRef> papers, Long authorId) {
    }

    /**
     * A comment to add to a project's or an idea's discussion.
     *
     * @param body the comment; must not be blank
     */
    public record CommentRequest(String body) {
    }

    /**
     * A comment in a project's or an idea's discussion.
     *
     * @param id the comment's id
     * @param body the text
     * @param authorName who wrote it, or {@code null}
     * @param createdAt when it was written
     */
    public record CommentView(Long id, String body, String authorName, OffsetDateTime createdAt) {
    }
}

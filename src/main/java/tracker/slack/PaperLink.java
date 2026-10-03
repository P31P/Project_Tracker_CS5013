package tracker.slack;

/** A paper link found in a Slack message: the URL as posted and its canonical form for deduplication. */
public record PaperLink(String url, String normalizedUrl) {
}

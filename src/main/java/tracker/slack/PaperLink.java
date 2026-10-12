package tracker.slack;

/**
 * A paper link found in a Slack message.
 *
 * @param url the link as posted
 * @param normalizedUrl its canonical form, used to store each paper once
 */
public record PaperLink(String url, String normalizedUrl) {
}

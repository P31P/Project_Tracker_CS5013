# 23rd September

- Created a Slack app to watch for research paper links shared in the project channel.
- Set it up so the app listens when someone mentions its bot.
- Created a testing channel and invited the app and bot.

![Testing channel on Slack where the bot can be mentioned](image.png)

# 5th October

- Connected Slack mentions to the database. The app saves each message, finds paper links in it, and records the papers and where they were shared.
- Added duplicate protection so Slack retrying the same message does not create extra records.
- Added two signed-in API routes to list saved papers and view one paper, with an optional status filter on the list.
- Added tests for repeated messages, messages with no links, messages with multiple links, and the papers API. The Slack API and paper repository are stubbed where useful in the tests.
- Updated the setup instructions and configuration notes for running Slack in Socket Mode.
- Fixed a flaky test found in review of PR #10. `IngestionReplayTest` saved papers and never rolled them back, so `SlackTablesTest.duplicatePaperUrlIsRejected` sometimes failed on a duplicate arXiv link when it ran second. The test is now `@Transactional` and the manual clean-up is removed.
- Checked the other database tests: they are `@Transactional` or use mocks, so nothing else leaked. Ran the suite in reverse order to confirm (45 tests pass).
- Added a line to `CLAUDE.md`: database tests must be `@Transactional` because all tests share one database.

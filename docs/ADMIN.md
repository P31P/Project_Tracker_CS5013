# Admin guide

For whoever runs the tracker for the lab. Setup and settings are in the [README](../README.md); this covers the jobs that come up afterwards.

## Set up the Slack app from scratch

Do this once, at <https://api.slack.com/apps> while signed in to the lab's workspace.

1. **Create New App → From scratch**. Name it and pick the workspace.
2. **Socket Mode**: turn it on. Slack asks for an app-level token; create one with the `connections:write` scope. This is `SLACK_APP_TOKEN` (starts with `xapp-`).
3. **OAuth & Permissions → Bot Token Scopes**: add `app_mentions:read` (to hear mentions) and `users:read` (to show who shared a paper; without it papers show no name). Install the app to the workspace. The **Bot User OAuth Token** is `SLACK_BOT_TOKEN` (starts with `xoxb-`).
4. **Event Subscriptions**: turn on, and under **Subscribe to bot events** add `app_mention`. With Socket Mode there is no request URL to fill in.
5. **Basic Information → Signing Secret** is `SLACK_SIGNING_SECRET`.
6. Put the three values and `SLACK_SOCKET_MODE_ENABLED=true` in the server's `.env` (never in git) and restart the app.

If you add a scope later, reinstall the app so the token picks it up.

## Add a Slack channel

The bot only hears mentions in channels it is a member of.

1. In the channel, type `/invite @<bot name>` (or **Channel details → Integrations → Add apps**). For a private channel, an existing member must do this.
2. Check it: post `@<bot name> https://arxiv.org/abs/1706.03762` and open the Papers tab. The paper should show a title within about 30 seconds.

To stop collecting from a channel, remove the bot from it. Papers already saved stay.

Only messages that mention the bot are saved, and only the links in them. Other chat is never stored.

## Fix a paper whose details are wrong or missing

Papers whose details can't be found are marked **Details not found**, and papers read from a PDF by AI are marked **Guessed by AI, please check**. Nothing is ever deleted.

1. Open the **Papers** tab and click **Details** on the paper.
2. Click **Fix details**, correct the title, authors, date or DOI, and save.

The paper is then marked as entered by hand and the background worker leaves it alone.

## Add someone to the allowlist

Only people with `allowlisted = true` in `app_user` can sign in. Add them with a new numbered migration in `src/main/resources/db/migration/` (see `V3__allowlist_team.sql`), merge it, and restart. Never edit a migration that has already been merged.

## Check a deployment

```bash
deploy/smoke-check.sh https://tracker.example.org
```

It checks that the sign-in page loads, that a signed-out page request is sent to sign-in, and that the API answers `401` when signed out, and it fails if any answer takes longer than 3 seconds (`MAX_SECONDS=5 deploy/smoke-check.sh …` changes the limit). Run it once right after a start, and again after the machine has sat idle for 30 minutes to see whether the first request is slow.

It cannot check the database or the Slack connection, because everything behind sign-in needs a Google login. Look for `Slack ingestion disabled` in the app log for a Slack problem, and check the Papers tab for the database.

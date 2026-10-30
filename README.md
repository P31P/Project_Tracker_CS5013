# Project_Tracker_CS5013
Course project for CS5013 : Programming with AI

A tracker for Raman Lab (DSAI, IIT Madras): it tracks projects and ideas, archives paper links from Slack automatically, and searches across all of them.

## Status
Google sign-in with an allowlist; database tables for users, projects, Slack messages and papers. Slack mentions can now be ingested into the paper archive, and the papers API is available. The dashboard has four tabs: **Home** (lab numbers, your projects, popular ideas, recent activity), **Projects** (list or drag-and-drop board; start with just a title, then add members, tags, links and timeline updates), **Ideas** (quick capture with tags, upvotes, turn an idea into a project) and **Papers**. Signed-out visitors land on a sign-in page; the account menu has Sign out. A background worker fills in each new paper's title, authors and date from Crossref (DOI links), arXiv, and the citation tags on publisher pages.

## Run locally
Needs Java 21, Maven and PostgreSQL with a `tracker` database (see [SCAFFOLDING.md](SCAFFOLDING.md), Step 0).
```bash
mvn verify
mvn spring-boot:run -Dspring-boot.run.profiles=local
```
Then open http://localhost:8080.

## API
Signed-out API calls get `401`; pages redirect to the sign-in page (`/login.html`).
- `GET /api/me`: email and name of the signed-in user
- `GET /api/papers`: list all archived papers with title, authors, date and who first shared them in Slack (optional `?status=PENDING|RESOLVED|UNRESOLVED`)
- `GET /api/papers/{id}`: single paper
- `GET /api/search?q=&type=`: projects, ideas and papers matching the text (web-search syntax: `"exact phrase"`, `or`, `-exclude`), best first, at most 50; `type` is optional (`PROJECT`, `IDEA` or `PAPER`)
- `GET /api/overview`: counts, the signed-in user's projects and the latest activity, for the Home tab
- `GET /api/people`: everyone who can sign in (to add as project members)
- `GET /api/projects`, `POST /api/projects` (`title` required; `description`, `status`, `tags`, `memberIds` optional), `GET`/`PATCH /api/projects/{id}`, `POST /api/projects/{id}/updates`, `POST /api/projects/{id}/links` (`url` must be http(s); `label` optional), `DELETE /api/projects/{id}/links/{linkId}`
- `GET /api/ideas`, `POST /api/ideas` (`title` required; `description`, `tags` optional), `PUT`/`DELETE /api/ideas/{id}/vote`, `POST /api/ideas/{id}/promote` (starts a project in Planning with the idea's tags)

Requests that change data need the `X-XSRF-TOKEN` header (the browser gets it from the `XSRF-TOKEN` cookie; `static/js/api.js` sends it). Errors come back as `{"error": "..."}`.

## Configuration
Settings live in `src/main/resources/application.yml`. Override them with env vars or a local `.env`: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `PORT`, `PAPER_RESOLUTION_ENABLED` (default `true`; set `false` to stop the worker calling Crossref and arXiv).

Sign-in uses Google: set `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` from a Google Cloud OAuth client (Web application, redirect URI `http://localhost:8080/login/oauth2/code/google`).

Slack ingestion (Socket Mode): set `SLACK_BOT_TOKEN`, `SLACK_SIGNING_SECRET`, `SLACK_APP_TOKEN` and `SLACK_SOCKET_MODE_ENABLED=true`. The app starts without these; omit them in CI. If Slack can't connect (bad token, outage), the error is logged and the rest of the app keeps running. To try it, start the app, mention the bot with a paper link in the channel, and check `GET /api/papers` (or the `paper` table): within about 30 s the paper has its title and authors. The poster's name needs the `users:read` bot scope (reinstall the app after adding it); without it papers are shown without a name. The app needs `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` to start; any placeholder value works if you only test Slack and read the database.

## Docs
- [PLAN.md](PLAN.md): where the plans are
- [plans/](plans/): shared phases/handoffs/schema and per-person plans
- [SCAFFOLDING.md](SCAFFOLDING.md): how to set up the project skeleton
- [requirements.txt](requirements.txt): what to install (grows stage by stage)
- [CLAUDE.md](CLAUDE.md): rules for AI coding assistants
- [Work_logs/](Work_logs/): what each of us has done

## License
[MIT](LICENSE) © 2026 Priyal Patel and Sujal Jayant Kumar

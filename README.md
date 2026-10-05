# Project_Tracker_CS5013
Course project for CS5013 : Programming with AI

A tracker for Raman Lab (DSAI, IIT Madras): it tracks projects and ideas, archives paper links from Slack automatically, and searches across all of them.

## Status
Google sign-in with an allowlist; database tables for users, projects, Slack messages and papers. Slack mentions can now be ingested into the paper archive, and the papers API is available. A background worker fills in each new paper's title, authors and date from Crossref (DOI links) and arXiv. The Papers tab UI is still in progress.

## Run locally
Needs Java 21, Maven and PostgreSQL with a `tracker` database (see [SCAFFOLDING.md](SCAFFOLDING.md), Step 0).
```bash
mvn verify
mvn spring-boot:run -Dspring-boot.run.profiles=local
```
Then open http://localhost:8080.

## API
- `GET /api/me`: email and name of the signed-in user
- `GET /api/papers`: list all archived papers (optional `?status=PENDING|RESOLVED|UNRESOLVED`)
- `GET /api/papers/{id}`: single paper

## Configuration
Settings live in `src/main/resources/application.yml`. Override them with env vars or a local `.env`: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `PORT`, `PAPER_RESOLUTION_ENABLED` (default `true`; set `false` to stop the worker calling Crossref and arXiv).

Sign-in uses Google: set `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` from a Google Cloud OAuth client (Web application, redirect URI `http://localhost:8080/login/oauth2/code/google`).

Slack ingestion (Socket Mode): set `SLACK_BOT_TOKEN`, `SLACK_SIGNING_SECRET`, `SLACK_APP_TOKEN` and `SLACK_SOCKET_MODE_ENABLED=true`. The app starts without these; omit them in CI. If Slack can't connect (bad token, outage), the error is logged and the rest of the app keeps running.

## Docs
- [PLAN.md](PLAN.md): where the plans are
- [plans/](plans/): shared phases/handoffs/schema and per-person plans
- [SCAFFOLDING.md](SCAFFOLDING.md): how to set up the project skeleton
- [requirements.txt](requirements.txt): what to install (grows stage by stage)
- [CLAUDE.md](CLAUDE.md): rules for AI coding assistants
- [Work_logs/](Work_logs/): what each of us has done

## License
[MIT](LICENSE) © 2026 Priyal Patel and Sujal Jayant Kumar

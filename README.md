# Project_Tracker_CS5013
Course project for CS5013 : Programming with AI

A tracker for Raman Lab (DSAI, IIT Madras): it tracks projects and ideas, archives paper links from Slack automatically, and searches across all of them.

## Status
Project skeleton in place (Spring Boot, Postgres, CI). Slack paper-link extraction (`tracker.slack.LinkExtractor`) is written and unit-tested; nothing is wired to Slack yet.

## Run locally
Needs Java 21, Maven and PostgreSQL with a `tracker` database (see [SCAFFOLDING.md](SCAFFOLDING.md), Step 0).
```bash
mvn verify
mvn spring-boot:run -Dspring-boot.run.profiles=local
```
Then open http://localhost:8080.

## Configuration
Settings live in `src/main/resources/application.yml`. Override them with env vars or a local `.env`: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `PORT`.

Sign-in uses Google: set `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` from a Google Cloud OAuth client (Web application, redirect URI `http://localhost:8080/login/oauth2/code/google`).

## Docs
- [PLAN.md](PLAN.md): where the plans are
- [plans/](plans/): shared phases/handoffs/schema and per-person plans
- [SCAFFOLDING.md](SCAFFOLDING.md): how to set up the project skeleton
- [requirements.txt](requirements.txt): what to install (grows stage by stage)
- [CLAUDE.md](CLAUDE.md): rules for AI coding assistants
- [Work_logs/](Work_logs/): what each of us has done

## License
[MIT](LICENSE) © 2026 Priyal Patel and Sujal Jayant Kumar

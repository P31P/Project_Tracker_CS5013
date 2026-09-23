# Agent Instructions

Instructions for any AI coding assistant working in this repo. `CLAUDE.md` and `AGENTS.md` are identical copies, so edit both together.

## Rules

1. **Never stage, commit or push.** Leave every change unstaged in the working tree so the team can review it. Don't run `git add`, `git commit` or `git push`.
2. **Minimal comments.** In code, config and markdown snippets, comment only where the reason isn't obvious. Don't explain every line.
3. **Update `README.md` as you go.** When a change adds something a user or developer would notice (a feature, a command, an env variable, a setup step), add one short line to the README. Keep it minimal.
4. **Add dependencies one at a time.** Add a library only when the current task needs it. Update `pom.xml` and `requirements.txt` together.
5. **Stay in your module.** Work inside the module you were asked about. If you must touch another owner's package or a shared file, say so.
6. **Keep it explainable.** Both team members must be able to explain every line, so choose simple, readable code over clever code.

## Project

**Unified Lab Project & Idea Tracking Platform**: CS5013 (Programming with AI) course project, team 47, built for Raman Lab (Dept. of DSAI, IIT Madras).

- **Projects & ideas:** log projects, post updates to a timeline, jot down early ideas. Every form has one required field (title).
- **Paper archive:** paper links posted in the lab's Slack are captured automatically. Title and authors are resolved in order: DOI/arXiv registries → citation meta tags → LLM for bare PDFs → `UNRESOLVED`. A link is never silently dropped.
- **Search:** one search bar across projects, ideas and papers, filterable by type.
- **AI auto-fill:** suggests title and tags from free text. Nothing is saved until the user confirms.

Context: `Proposal_Doc.pdf`, `Design_Doc.pdf`. Plans: `PLAN.md`, `plans/PRIYAL.md`, `plans/SUJAL.md`. Skeleton guide: `SCAFFOLDING.md`.

## Team & ownership

| Module | Package(s) | Owner |
|---|---|---|
| A: Slack ingestion | `slack` | Priyal Patel (`P31P`) |
| B: Paper archive | `papers` | Priyal Patel |
| C: Search & paper views | `search`, `static/js/papers.js`, `search.js` | Priyal Patel |
| D: Auth & data layer | `auth`, `data`, `config`, `common` | Sujal Jayant Kumar (`SujalKumar06`) |
| E: Projects & ideas | `projects`, `static/js/projects.js`, `ideas.js` | Sujal Jayant Kumar |
| F: AI auto-fill | `llm`, `autofill`, `static/js/autofill.js` | Sujal Jayant Kumar |
| G: Deployment & handoff | `deploy/`, `docs/` | Both |

Shared files where changes need care: `pom.xml`, `application.yml`, `static/js/app.js`, and any migration that touches another owner's tables.

## Stack

- Java 21, Maven (`./mvnw`), Spring Boot 4.1. Java is required by the course, so don't add other backend languages.
- PostgreSQL 16+, Flyway migrations, Spring Data JPA.
- Slack Bolt for Java over Socket Mode (no public URL needed).
- Frontend: plain JS in `src/main/resources/static/`. No Node, no build step, served from the same jar.
- Deployment: one jar with config kept outside it. No Docker.
- Tests: JUnit 5, Zonky embedded Postgres (a real Postgres, **never H2**), WireMock for external HTTP.

## Commands

These work once the scaffold from `SCAFFOLDING.md` is in place.

```bash
./mvnw verify
./mvnw test -Dtest=ClassName
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Local config comes from `.env` (copy `.env.example`).

## Conventions

- Base package: `in.ac.iitm.ramanlab.tracker`.
- Branches: `<module letter>/<topic>`, e.g. `a/slack-events`, `d/oauth-allowlist`.
- Migrations: never edit a merged Flyway file. `V1__init.sql` first, then date-based versions like `V20261017__search_indexes.sql`.
- Secrets only in env vars / `.env` (git-ignored). Never hardcode tokens, keys or passwords.
- Projects and ideas require only a title. Every other column is nullable.
- All LLM calls go through `llm.LlmClient`. Every feature must still work with `llm.provider=disabled`.
- Slack events can be delivered twice. `UNIQUE(channel_id, message_ts)` makes the second delivery a no-op.
- Database tests run against real Postgres. External API tests use recorded responses in `src/test/resources/fixtures/`.

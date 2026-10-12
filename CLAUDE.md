# Agent Instructions

Instructions for any AI coding assistant working in this repo.

## Rules

1. **Never stage, commit or push.** Leave every change unstaged in the working tree so the team can review it. Don't run `git add`, `git commit` or `git push`.
2. **Minimal comments.** In code, config and markdown snippets, comment only where the reason isn't obvious. Don't explain every line.
3. **Update `README.md` as you go.** When a change adds something a user or developer would notice (a feature, a command, an env variable, a setup step), add one short line to the README. Keep it minimal.
4. **Build sequentially.** Do only what the current task needs. Don't add packages, config, interfaces or dependencies for features that aren't being built yet. When a feature lands, add its conventions to this file.
5. **Add dependencies one at a time.** Update `pom.xml` and `requirements.txt` together.
6. **Stay in your module.** Work inside the module you were asked about. If you must touch another owner's code or a shared file, say so.
7. **Keep it explainable.** Both team members must be able to explain every line, so choose simple, readable code over clever code.

## Project

**Unified Lab Project & Idea Tracking Platform**: CS5013 (Programming with AI) course project, team 47, built for Raman Lab (Dept. of DSAI, IIT Madras). It tracks lab projects and ideas, archives paper links shared in Slack, and searches across them.

Context: `Proposal_Doc.pdf`, `Design_Doc.pdf`. Plans: `plans/SHARED.md`, `plans/PRIYAL.md`, `plans/SUJAL.md`. Skeleton guide: `SCAFFOLDING.md`.

**Current stage:** Phase 1 done on the UI side. Home, Projects (list and board, members, tags, links), Ideas (tags, upvotes) and Papers tabs, sign-in page and sign-out are built; search (Priyal), auto-fill and deployment are next.

## Team & ownership

| Module | Owner |
|---|---|
| A: Slack ingestion, B: Paper archive, C: Search & paper views | Priyal Patel (`P31P`) |
| D: Auth & data layer, E: Projects & ideas, F: AI auto-fill | Sujal Jayant Kumar (`SujalKumar06`) |
| G: Deployment & handoff | Both |

Shared files where changes need care: `pom.xml`, `application.yml`.

## Stack

- Java 21, Maven, Spring Boot 4.1. Java is required by the course, so don't add other backend languages.
- PostgreSQL 16+, Flyway migrations, Spring Data JPA.
- Frontend: plain JS in `src/main/resources/static/`. No Node, no build step.
- No Docker.
- Tests: JUnit 5 with Zonky embedded Postgres (a real Postgres, **never H2**).

## Commands

These work once the scaffold from `SCAFFOLDING.md` is in place.

```bash
mvn verify
mvn test -Dtest=ClassName
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Local overrides and secrets go in a git-ignored `.env` file.

## Conventions

- Base package: `tracker`. Create a package when its first class is written.
- Branches: short topic names, e.g. `scaffold`, `slack-events`, `oauth-allowlist`.
- Migrations: numbered in order, `V1__init.sql`, `V2__create_slack_tables.sql`, `V3__…`. Take the next number after the highest on `main`; if someone merges that number first, renumber yours before merging. Never edit a merged migration.
- `paper.normalized_url` comes from `LinkExtractor`: arXiv links become `https://arxiv.org/abs/<id>` (no version) and DOI/Springer/ACM/Wiley/Nature links become `https://doi.org/<lowercase doi>`.
- Entities and repositories live in `tracker.data`. Add repository methods only when a feature needs them.
- API errors: throw `ResponseStatusException` with a readable reason; `tracker.common.ApiErrors` turns it into `{"error": "..."}`. Find the signed-in user's `app_user` row with `tracker.auth.CurrentUser`.
- CSRF is on (`csrf().spa()`): POST/PATCH/DELETE need the `X-XSRF-TOKEN` header, which `sendJson` adds. In MockMvc tests add `.with(csrf())`.
- Every page except the sign-in page (`static/login.html`) and `/css/**` requires Google sign-in (`tracker.auth.SecurityConfig`). Only `app_user` rows with `allowlisted = true` get in; add people with a new migration.
- Tests that need the database extend `tracker.support.PostgresTestBase` and are `@Transactional`, so they roll back. All tests share one database; a test that commits leaks rows into the others. Tests that need a signed-in user use `oidcLogin()` from spring-security-test.
- A scheduled worker resolves PENDING papers (`tracker.papers.PaperResolutionWorker`). It is off in tests (`PostgresTestBase`); a test that needs it turns it on with `papers.resolution-enabled=true`.
- Paper resolvers are `PaperResolver` beans tried in `@Order`: Crossref, arXiv, then page meta tags. A new rung is a new bean with the next `@Order`.
- Frontend modules export a `render…(container)` function and build the page with `textContent`/DOM calls, never `innerHTML`, because titles and names come from outside sources. `static/js/app.js` registers each module as a tab with `registerTab(id, label, render, description)` and calls `render(container, { actions, heading, description, path })`: a fresh, empty container, the page header's action slot and text, and anything after the tab id in the URL (`#projects/12` gives `path` "12"). Use `getJson`/`sendJson` from `static/js/api.js` and the helpers in `static/js/ui.js`; `/api/**` answers `401` when signed out and `api.js` sends the user to sign-in.
- Tags on projects and ideas are a `text[]` column, stored trimmed, lowercased, without `#` and without duplicates (`ProjectService.tags`). The frontend colours a tag or a person from the text (`hueOf` in `ui.js`), so the same tag always has the same colour.
- Icons come from `static/js/icons.js` (`icon(name)`, built with `createElementNS`). Add a new icon as a path list there; don't paste SVG markup.
- Links shown as a `href` must be `http(s)`: the server rejects anything else and the frontend checks again before setting `href`.
- Secrets only in env vars / `.env` (git-ignored). Never hardcode tokens, keys or passwords.

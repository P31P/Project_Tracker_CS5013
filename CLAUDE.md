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

**Current stage:** Phase 2 done (seed data still to do). Phase 3 is the lab's feedback after the demo: more project and idea fields with built-in tags, member profiles with interest-based activity on Home, a Lab Manager role that gives and removes access, a People tab, and search filters (see `plans/SHARED.md`). Deployment is Phase 4.

## Team & ownership

| Module | Owner |
|---|---|
| A: Slack ingestion, B: Paper archive, C: Search, People, profiles & paper views | Priyal Patel (`P31P`) |
| D: Auth, roles & data layer, E: Projects & ideas, F: AI auto-fill | Sujal Jayant Kumar (`SujalKumar06`) |
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
- Javadoc on every public class, record, constructor and method (and package-private helpers other classes call): a one-sentence summary, then `@param`, `@return` and `@throws` where they apply; record components go in `@param`. Overrides inherit the interface's doc. Keep it short; inline `//` comments stay for non-obvious reasons only.
- Branches: short topic names, e.g. `scaffold`, `slack-events`, `oauth-allowlist`.
- Migrations: numbered in order, `V1__init.sql`, `V2__create_slack_tables.sql`, `V3__…`. Take the next number after the highest on `main`; if someone merges that number first, renumber yours before merging. Never edit a merged migration.
- `paper.normalized_url` comes from `LinkExtractor`: arXiv links become `https://arxiv.org/abs/<id>` (no version) and DOI/Springer/ACM/Wiley/Nature links become `https://doi.org/<lowercase doi>`.
- Entities and repositories live in `tracker.data`. Add repository methods only when a feature needs them.
- API errors: throw `ResponseStatusException` with a readable reason; `tracker.common.ApiErrors` turns it into `{"error": "..."}`. Find the signed-in user's `app_user` row with `tracker.auth.CurrentUser`.
- CSRF is on (`csrf().spa()`): POST/PATCH/DELETE need the `X-XSRF-TOKEN` header, which `sendJson` adds. In MockMvc tests add `.with(csrf())`.
- Every page except the sign-in page (`static/login.html`), `/css/**` and `/fonts/**` requires Google sign-in (`tracker.auth.SecurityConfig`). Only `app_user` rows with `allowlisted = true` get in; add people with a new migration.
- Tests that need the database extend `tracker.support.PostgresTestBase` and are `@Transactional`, so they roll back. All tests share one database; a test that commits leaks rows into the others. Tests that need a signed-in user use `oidcLogin()` from spring-security-test.
- `src/test/resources/seed-mock-data.sql` holds demo data for local databases only. `SeedDataTest` loads it in every build, so a migration that breaks the script fails the build: update the script in the same PR.
- A scheduled worker resolves PENDING papers (`tracker.papers.PaperResolutionWorker`). It is off in tests (`PostgresTestBase`); a test that needs it turns it on with `papers.resolution-enabled=true`.
- Paper resolvers are `PaperResolver` beans tried in `@Order`: Crossref, arXiv, page meta tags, then `PdfLlmResolver`, which reads a bare PDF's first two pages with the language model (only when `llm.enabled`; `source = MODEL`). A new rung is a new bean with the next `@Order`. `ResolutionLadderTest` runs the whole ladder against WireMock.
- Frontend modules export a `render…(container)` function and build the page with `textContent`/DOM calls, never `innerHTML`, because titles and names come from outside sources. `static/js/app.js` registers each module as a tab with `registerTab(id, label, render, description)` and calls `render(container, { actions, heading, description, path })`: a fresh, empty container, the page header's action slot and text, and anything after the tab id in the URL (`#projects/12` gives `path` "12"). Use `getJson`/`sendJson` from `static/js/api.js` and the helpers in `static/js/ui.js`; `/api/**` answers `401` when signed out and `api.js` sends the user to sign-in.
- Language models go through `tracker.llm.LlmClient.completeJson(prompt, schema)`: one OpenAI-style client for a local Ollama (the default) or a hosted API, set by `llm.*` (`LLM_ENABLED`, off by default and in tests). It throws `LlmException` when the model is off, down, slow or gives no answer. Prompts live in `src/main/resources/prompts/`. Treat the answer as untrusted: check every field (see `AutoFillService.read`). Tests use WireMock as the model.
- Auto-fill (`tracker.autofill`) only suggests; it never saves. A bad or missing answer gives 503 "Couldn't suggest…", never a 500, and the Suggest button (`static/js/autofill.js`) is hidden when auto-fill is off.
- Search is one SQL query in `tracker.search.SearchService` over the generated `search` tsvector columns (V9) of `project`, `idea` and `paper`. A new searchable table needs its own `search` column, a GIN index and a branch in that query.
- A resolver can return a reference list (`ResolvedMetadata.references`); only `CrossrefResolver` does. `PaperResolutionService` replaces a paper's `paper_reference` rows each time it resolves the paper.
- Tags on projects and ideas are a `text[]` column, stored trimmed, lowercased, without `#` and without duplicates (`ProjectService.tags`). The frontend colours a tag or a person from the text (`hueOf` in `ui.js`), so the same tag always has the same colour.
- User-written text (descriptions, updates, comments) is Markdown: show it with `markdown(text)` from `static/js/markdown.js`, which builds DOM nodes and only makes `http(s)` links; use `plainText(text)` for clamped previews. Never turn Markdown into an HTML string. Add new syntax to `markdown.js` itself.
- Design: restrained and calm. Colours are OKLCH tokens with neutrals tinted towards the accent's blue; the accent is for the main action and links, not decoration. No gradients, glows, coloured side stripes or icon tiles above numbers. Type is Geist (`static/fonts/`, loaded by `@font-face` in `app.css`); numbers that line up or change use `tabular-nums`.
- Motion: use `--ease-out` and `--fast` (about 150–200 ms). Controls press to `scale(0.97)`; dialogs, the account menu and toasts enter from `@starting-style` (dialogs from `scale(0.96)`, never from 0). Don't animate things that happen often, such as lists redrawing after a vote or filter. Put hover styles inside `@media (hover: hover) and (pointer: fine)`. `prefers-reduced-motion` turns transitions off for the whole page.
- Colours are CSS variables on `:root` in `app.css`. The dark values appear twice: under `prefers-color-scheme: dark` (when no theme is picked) and under `:root[data-theme="dark"]` (picked with the switch beside the account menu, `static/js/theme.js`); change both together. Each page's `<head>` applies the saved theme before drawing, so the inline script there must stay first.
- Icons come from `static/js/icons.js` (`icon(name)`, built with `createElementNS`). Add a new icon as a path list there; don't paste SVG markup.
- Links shown as a `href` must be `http(s)`: the server rejects anything else and the frontend checks again before setting `href`.
- Secrets only in env vars / `.env` (git-ignored). Never hardcode tokens, keys or passwords.

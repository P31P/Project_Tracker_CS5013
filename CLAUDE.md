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

**Current stage:** Phase 2 mostly done. Home, Projects (list and board, members, tags, links), Ideas (tags, upvotes, discussion), Papers (detail page, Fix details) and search are built, with a light/dark theme switch, sign-in and sign-out, and auto-fill (off by default). The PDF step of the paper resolver, paper references, seed data and deployment are next.

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
- Javadoc on every public class, record, constructor and method (and package-private helpers other classes call): a one-sentence summary, then `@param`, `@return` and `@throws` where they apply; record components go in `@param`. Overrides inherit the interface's doc. Keep it short; inline `//` comments stay for non-obvious reasons only.
- Branches: short topic names, e.g. `scaffold`, `slack-events`, `oauth-allowlist`.
- Migrations: numbered in order, `V1__init.sql`, `V2__create_slack_tables.sql`, `V3__…`. Take the next number after the highest on `main`; if someone merges that number first, renumber yours before merging. Never edit a merged migration.
- `paper.normalized_url` comes from `LinkExtractor`: arXiv links become `https://arxiv.org/abs/<id>` (no version) and DOI/Springer/ACM/Wiley/Nature links become `https://doi.org/<lowercase doi>`.
- Entities and repositories live in `tracker.data`. Add repository methods only when a feature needs them.
- API errors: throw `ResponseStatusException` with a readable reason; `tracker.common.ApiErrors` turns it into `{"error": "..."}`. Find the signed-in user's `app_user` row with `tracker.auth.CurrentUser`.
- CSRF is on (`csrf().spa()`): POST/PATCH/DELETE need the `X-XSRF-TOKEN` header, which `sendJson` adds. In MockMvc tests add `.with(csrf())`.
- Every page except the sign-in page (`static/login.html`), `/css/**` and `/fonts/**` requires Google sign-in (`tracker.auth.SecurityConfig`). Only `app_user` rows with `allowlisted = true` get in, checked at sign-in and again on every request (`AccessCheckFilter`), so removing someone signs them out. People are added and removed on the Manage access page (`tracker.auth.AccessService`, `static/js/access.js`) by a Lab Manager or an admin (`app_user.role`: `MEMBER`, `LAB_MANAGER`, `ADMIN`); use a migration only for the lab's first admin and Lab Manager.
- Tests that need the database extend `tracker.support.PostgresTestBase` and are `@Transactional`, so they roll back. All tests share one database; a test that commits leaks rows into the others. Tests that need a signed-in user use `oidcLogin()` from spring-security-test.
- `src/test/resources/seed-mock-data.sql` holds demo data for local databases only. `SeedDataTest` loads it in every build, so a migration that breaks the script fails the build: update the script in the same PR.
- A scheduled worker resolves PENDING papers (`tracker.papers.PaperResolutionWorker`). It is off in tests (`PostgresTestBase`); a test that needs it turns it on with `papers.resolution-enabled=true`.
- Paper resolvers are `PaperResolver` beans tried in `@Order`: Crossref, arXiv, page meta tags, then `PdfLlmResolver`, which reads a bare PDF's first two pages with the language model (only when `llm.enabled`; `source = MODEL`). A new rung is a new bean with the next `@Order`. `ResolutionLadderTest` runs the whole ladder against WireMock.
- Frontend modules export a `render…(container)` function and build the page with `textContent`/DOM calls, never `innerHTML`, because titles and names come from outside sources. `static/js/app.js` registers each module as a tab with `registerTab(id, label, render, description)` and calls `render(container, { actions, heading, description, path })`: a fresh, empty container, the page header's action slot and text, and anything after the tab id in the URL (`#projects/12` gives `path` "12"). Use `getJson`/`sendJson` from `static/js/api.js` and the helpers in `static/js/ui.js`; `/api/**` answers `401` when signed out and `api.js` sends the user to sign-in.
- Language models go through `tracker.llm.LlmClient.completeJson(prompt, schema)`: one OpenAI-style client for a local Ollama (the default) or a hosted API, set by `llm.*` (`LLM_ENABLED`, off by default and in tests). It throws `LlmException` when the model is off, down, slow or gives no answer. Prompts live in `src/main/resources/prompts/`. Treat the answer as untrusted: check every field (see `AutoFillService.read`). Tests use WireMock as the model.
- Auto-fill (`tracker.autofill`) only suggests; it never saves. It also suggests the five parts of the write-up and offers the model the lab's built-in tags (`{{tags}}` in `prompts/autofill.txt`); the frontend fills only boxes that are still empty (`fillDetails` in `details.js`). A bad or missing answer gives 503 "Couldn't suggest…", never a 500, and the Suggest button (`static/js/autofill.js`) is hidden when auto-fill is off.
- Search is one SQL query in `tracker.search.SearchService` over the generated `search` tsvector columns (V9; paper's rebuilt with its tags in V13; project's and idea's with their write-up in V19) of `project`, `idea` and `paper`. A new searchable table needs its own `search` column, a GIN index and a branch in that query, with the same filters (`SearchFilters`: tags, person, dates). A generated column can't be altered: drop and re-add it in a new migration. Papers aren't linked to app users, so the person filter leaves them out. The Search tab keeps the search in the address (`#search/q=…&tag=…`).
- Papers have tags (`paper.tags`, tidied by `ProjectService.tags`, set with `PUT /api/papers/{id}/tags`, which leaves status and source alone), upvotes (`paper_vote`) and read marks (`paper_read`). Votes and read marks go through `tracker.papers.PaperMarks`; a read mark is private to its person, so never show someone else's.
- Profiles: `app_user.bio`, `background` and `interests` (`text[]`, tidied like tags), read and saved through `tracker.profile` (`GET`/`PATCH /api/profile`). `ProfileView` is what other pages show of a person. "For you" on Home comes from `tracker.profile.InterestMatches`, worked out on each load (nothing stored); `static/js/profile.js` has the profile card.
- People tab: `tracker.people` (`GET /api/people/directory`, `GET /api/people/{id}`) lists only allowlisted people. `GET /api/people` stays the short list for member pickers (`tracker.projects.PeopleController`). `profile.css` styles both profiles and the People tab (`loadProfileStyles()` in `profile.js`).
- A resolver can return a reference list (`ResolvedMetadata.references`); only `CrossrefResolver` does. `PaperResolutionService` replaces a paper's `paper_reference` rows each time it resolves the paper.
- Projects and ideas share a longer write-up, `tracker.data.ResearchDetails` (embedded: `core_question`, `motivation`, `approach`, `resources`, `challenges`), sent as `details` and tidied by `ProjectService.details`. Both have `open_for_collab`; a project also has `started_on` and `collab_opened_at` (set by `setOpenForCollab`). Statuses read Planning / Ongoing / On hold / Completed (`ProjectStatus.label` and `STATUSES` in `projects.js`); the stored values stay `PLANNING`/`ACTIVE`/`ON_HOLD`/`DONE`. The frontend's form fields and sections for the write-up are in `static/js/details.js`.
- References on projects and ideas are web links (`project_link`, `idea_link`, checked by `ProjectService.checkedLink`) and papers from the archive (`project_paper`, `idea_paper`, many-to-many `papers` on the entities). The frontend's lists, forms and archive search for them are in `static/js/references.js`; a project's discussion is `static/js/discussion.js`.
- Tags on projects and ideas are a `text[]` column, stored trimmed, lowercased, without `#` and without duplicates (`ProjectService.tags`). The lab's built-in tags are rows in the `tag` table (V15, with a display `label` and a group); any other tag is a custom tag and is still allowed. Forms pick tags with `tagPicker(name, selected)` from `static/js/tags.js`, which keeps a hidden input `name` up to date and has `suggest(tags)` for auto-fill. The frontend colours a tag or a person from the text (`hueOf` in `ui.js`), so the same tag always has the same colour.
- User-written text (descriptions, updates, comments) is Markdown: show it with `markdown(text)` from `static/js/markdown.js`, which builds DOM nodes and only makes `http(s)` links; use `plainText(text)` for clamped previews. Never turn Markdown into an HTML string. Add new syntax to `markdown.js` itself.
- Design: restrained and calm. Colours are OKLCH tokens with neutrals tinted towards the accent's blue; the accent is for the main action and links, not decoration. No gradients, glows, coloured side stripes or icon tiles above numbers. Type is Geist (`static/fonts/`, loaded by `@font-face` in `app.css`); numbers that line up or change use `tabular-nums`.
- Motion: use `--ease-out` and `--fast` (about 150–200 ms). Controls press to `scale(0.97)`; dialogs, the account menu and toasts enter from `@starting-style` (dialogs from `scale(0.96)`, never from 0). Don't animate things that happen often, such as lists redrawing after a vote or filter. Put hover styles inside `@media (hover: hover) and (pointer: fine)`. `prefers-reduced-motion` turns transitions off for the whole page.
- Colours are CSS variables on `:root` in `app.css`. The dark values appear twice: under `prefers-color-scheme: dark` (when no theme is picked) and under `:root[data-theme="dark"]` (picked with the switch beside the account menu, `static/js/theme.js`); change both together. Each page's `<head>` applies the saved theme before drawing, so the inline script there must stay first.
- Icons come from `static/js/icons.js` (`icon(name)`, built with `createElementNS`). Add a new icon as a path list there; don't paste SVG markup.
- Links shown as a `href` must be `http(s)`: the server rejects anything else and the frontend checks again before setting `href`.
- Secrets only in env vars / `.env` (git-ignored). Never hardcode tokens, keys or passwords.

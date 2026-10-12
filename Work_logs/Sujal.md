# 23rd September

- Wrote the project plan from the proposal and design doc: shared phases, handoffs and schema draft (`plans/SHARED.md`), and a task list for each of us (`plans/PRIYAL.md`, `plans/SUJAL.md`).
- Added `CLAUDE.md` with rules for AI assistants: no commits by the assistant, minimal comments, build sequentially, add dependencies one at a time.
- Wrote `SCAFFOLDING.md`, a step-by-step guide for the project skeleton.

# 28th September

- Built the project skeleton (H1): Spring Boot 4.1 on Java 21 with Maven, PostgreSQL config, Flyway, a placeholder page and GitHub Actions CI.
- Tests run against an embedded Postgres 16 (no Docker); `mvn verify` passes and the app starts on `localhost:8080`.
- Added the MIT License under both our names.

# 3rd October

- First database migration (`V1__init.sql`): `app_user` (unique email, allowlist flag) and `project` (only the title is required, blank titles rejected).
- JPA entities and repositories for both tables in `tracker.data`.
- `ConstraintsTest` checks against a real Postgres that a project saves with only a title, a blank title is rejected, and duplicate emails are rejected.
- Reviewed Priyal's `LinkExtractor` PR (#4); found DOI links with extra path parts (e.g. Springer `/fulltext.html`) being stored as different papers.

# 4th October

- Second migration (`V2__create_slack_tables.sql`): `slack_message`, `paper` and `paper_share`, matching Priyal's `LinkExtractor` output, with entities, repositories and `SlackTablesTest` (duplicates rejected, new papers start `PENDING`).
- Switched migrations to plain numbers (`V1`, `V2`, `V3`…) instead of dates.
- Set up the Google Cloud OAuth client (testing mode, both of us as test users).
- Google sign-in: every page requires sign-in, and only allowlisted `app_user` rows get in (`SecurityConfig`, `AllowlistService`). `V3__allowlist_team.sql` allowlists both of us.
- `GET /api/me` returns the signed-in user's email and name.
- Tests for the allowlist, the sign-in redirect and `/api/me` (37 tests in total).
- Opened PRs as a stack: #5 (users, projects) → #6 (Slack tables) → sign-in.

# 12th October

- Dashboard shell (H7): `index.html` (header, tab bar, content area), `app.css` (light and dark themes, phone layout), `api.js` (`getJson`, sends the user to sign-in on `401`) and `app.js` (`registerTab`, tab from the URL hash, signed-in name from `/api/me`). Priyal's Papers tab is registered and shows up.
- `/api/**` now answers `401` when signed out instead of redirecting, so the page's JavaScript can react; pages still redirect to Google. Added a test that the sign-in error page still loads, so a refused user can't end up in a redirect loop.
- One-line change to Priyal's `papers.css`: paper cards use the shell's surface colour.
- Sign-in page (`login.html`): signed-out visitors land on a short welcome card with a "Sign in with Google" button instead of going straight to Google; a refused account sees a clear message there.
- Redesigned the shell and sign-in page for the mid demo: logo, frosted header, account menu with sign-out, page headers, toasts, two-column sign-in page with feature highlights; light and dark themes.
- CSRF protection for the API (`csrf().spa()` plus a filter so the token cookie is always present); sign-out goes back to the sign-in page.
- Projects (module E): `V5` (statuses, `updated_at`, `project_update` timeline), entities, `ProjectService`/`ProjectController`, errors as `{"error": ...}`. Projects tab: list with status filters, "New project" dialog with only the title required, project page with status picker, edit dialog, update composer and timeline (status changes are logged on it).
- Ideas (module E): `V6`, `IdeaService`/`IdeaController`, promote an idea to a Planning project (only once). Ideas tab: quick capture, filters, "Turn into a project".
- 14 new API tests (99 in total). Drove both tabs end to end in headless Chrome against a stand-in API (create, validation message, update, status change, capture, promote); found and fixed a "null" showing for projects without a description.
- More to the project side: `V7` adds project members, tags and links (only `http(s)` links are accepted), `V8` adds idea tags and upvotes (one vote per person). `GET /api/people` lists who can be added to a project.
- Home tab (`GET /api/overview`): counts per status, open ideas and papers; the projects I own or am in; popular ideas; a feed of recent updates, status changes, new projects, ideas and papers.
- Projects tab: List/Board switch (remembered per browser), drag a card to another column to change its status, filter by tag. The project page has a side panel with status, members, tags and links. Ideas tab: tags, upvotes, sort by most votes.
- Look: line icons (`icons.js`), coloured avatars and tags, stat cards, cards that fade in (off when the system asks for reduced motion).
- 10 new API tests (109 in total). Drove Home, the board (drag and drop, tag filter), links, members, voting and promoting end to end in headless Chrome against a stand-in API, and checked light, dark and phone layouts.


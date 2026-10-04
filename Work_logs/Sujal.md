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

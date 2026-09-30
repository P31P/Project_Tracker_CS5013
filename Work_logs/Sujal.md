# 23rd September

- Wrote the project plan from the proposal and design doc: shared phases, handoffs and schema draft (`plans/SHARED.md`), and a task list for each of us (`plans/PRIYAL.md`, `plans/SUJAL.md`).
- Added `CLAUDE.md` with rules for AI assistants: no commits by the assistant, minimal comments, build sequentially, add dependencies one at a time.
- Wrote `SCAFFOLDING.md`, a step-by-step guide for the project skeleton.

# 28th September

- Built the project skeleton (H1): Spring Boot 4.1 on Java 21 with Maven, PostgreSQL config, Flyway, a placeholder page and GitHub Actions CI.
- Tests run against an embedded Postgres 16 (no Docker); `mvn verify` passes and the app starts on `localhost:8080`.
- Added the MIT License under both our names.

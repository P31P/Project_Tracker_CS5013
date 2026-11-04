# Shared: phases, handoffs, schema

What both of you depend on. Individual tasks are in [PRIYAL.md](PRIYAL.md) and [SUJAL.md](SUJAL.md).

## Phases

| Phase | Works at the end |
|---|---|
| 0: Foundations | Skeleton builds, schema exists, allowlisted members can sign in, Slack app receives messages |
| 1: Core flows | A link posted in Slack shows up in the Papers tab; a member can create a project and post updates |
| 2: Complete features | Full paper resolution, search, ideas, auto-fill |
| 3: Deploy | Runs on the lab's machine, installable from the README |

## Handoffs

| # | From → to | What | Needed before |
|---|---|---|---|
| H1 | Sujal → Priyal | Maven skeleton (`SCAFFOLDING.md`), CI | any code |
| H2 | Sujal → Priyal | `LlmClient` + `DisabledLlmClient` | rung 3 |
| H3 | Priyal → Sujal | Review of `slack_message`, `paper`, `paper_share` (PR #6) | H4 |
| H4 | Sujal → Priyal | Migrations V1–V2, entities, repositories | ingestion, worker |
| H5 | Priyal → Sujal | Slack app tokens + test-channel access (outside git) | running the full flow locally |
| H6 | Sujal → Priyal | `SecurityConfig` + test helper for signed-in MockMvc | `PaperController`, `SearchController` |
| H7 | Sujal → Priyal | Dashboard shell: `index.html`, `api.js`, `app.js` with `registerTab(id, label, render)` | `papers.js`, `search.js` |
| H8 | Sujal → Priyal | `HostedLlmClient` | running rung 3 for real |
| H9 | Sujal → Priyal | `src/test/resources/seed-mock-data.sql` | `SearchServiceTest` |
| H10 | Priyal → Sujal | Search migration (touches `project` and `idea`) | search |

## Schema

### Built

| Table | Columns | Migration |
|---|---|---|
| `app_user` | email UNIQUE, name, allowlisted, created_at | V1, team allowlisted in V3 |
| `project` | title **NOT NULL** and not blank, description, status, owner_id, created_at | V1 |
| `slack_message` | channel_id, message_ts, slack_user_id, slack_user_name, text, permalink, received_at, **UNIQUE(channel_id, message_ts)** | V2 |
| `paper` | normalized_url UNIQUE, status (`PENDING`/`RESOLVED`/`UNRESOLVED`), created_at | V2 |
| `paper` (added) | title, authors, published_on, doi, source, attempts, next_attempt_at | V4 |
| `paper_share` | paper_id, slack_message_id, posted_url, **UNIQUE(paper_id, slack_message_id)** | V2 |
| `project` (changed) | status now NOT NULL, `PLANNING`/`ACTIVE`/`ON_HOLD`/`DONE` (default `ACTIVE`); added updated_at | V5 |
| `project_update` | project_id, author_id, kind (`NOTE`/`STATUS`), body (not blank), created_at | V5 |
| `idea` | title **NOT NULL** and not blank, description, author_id, project_id (set when promoted), created_at | V6 |
| `project` (changed) | added tags `text[]` | V7 |
| `project_member` | project_id, user_id (primary key on both) | V7 |
| `project_link` | project_id, label (not blank), url (must start `http://` or `https://`), created_at | V7 |
| `idea` (changed) | added tags `text[]` | V8 |
| `idea_vote` | idea_id, user_id (primary key on both, so one vote per person), created_at | V8 |
| `project`, `idea`, `paper` (added) | `search` generated `tsvector` with a GIN index (title weighted highest, then description or authors, then tags or DOI); `immutable_join()` function | V9 |
| `idea_comment` | idea_id, author_id, body (not blank), created_at | V10 |
| `paper_reference` | paper_id, position (unique per paper), citation (not blank), doi | V11 |

### Planned (added by the feature that needs it)

| Table / columns | With |
|---|---|
| (nothing planned right now) | |

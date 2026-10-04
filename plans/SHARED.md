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
| H9 | Sujal → Priyal | `scripts/seed-mock-data.sql` | `SearchServiceTest` |
| H10 | Priyal → Sujal | Search migration (touches `project` and `idea`) | search |

## Schema

### Built

| Table | Columns | Migration |
|---|---|---|
| `app_user` | email UNIQUE, name, allowlisted, created_at | V1, team allowlisted in V3 |
| `project` | title **NOT NULL** and not blank, description, status, owner_id, created_at | V1 |
| `slack_message` | channel_id, message_ts, slack_user_id, slack_user_name, text, permalink, received_at, **UNIQUE(channel_id, message_ts)** | V2 |
| `paper` | normalized_url UNIQUE, status (`PENDING`/`RESOLVED`/`UNRESOLVED`), created_at | V2 |
| `paper_share` | paper_id, slack_message_id, posted_url, **UNIQUE(paper_id, slack_message_id)** | V2 |

### Planned (added by the feature that needs it)

| Table / columns | With |
|---|---|
| `project_update` | project timeline |
| `idea` | idea logging |
| `paper.title`, `paper.authors`, `paper_reference` | paper resolver |

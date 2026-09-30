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
| H3 | Priyal → Sujal | Review of `slack_message`, `paper`, `paper_share`, `paper_reference` in the V1 draft | H4 |
| H4 | Sujal → Priyal | `V1__init.sql`, entities, repositories | ingestion, worker |
| H5 | Priyal → Sujal | Slack app tokens + test-channel access (outside git) | running the full flow locally |
| H6 | Sujal → Priyal | `SecurityConfig` + test helper for signed-in MockMvc | `PaperController`, `SearchController` |
| H7 | Sujal → Priyal | Dashboard shell: `index.html`, `api.js`, `app.js` with `registerTab(id, label, render)` | `papers.js`, `search.js` |
| H8 | Sujal → Priyal | `HostedLlmClient` | running rung 3 for real |
| H9 | Sujal → Priyal | `scripts/seed-mock-data.sql` | `SearchServiceTest` |
| H10 | Priyal → Sujal | Search migration (touches `project` and `idea`) | search |

## Schema v1 draft (H3 → H4)

Sujal writes `V1__init.sql`; Priyal reviews the Slack and paper tables.

| Table | Key columns | Notes |
|---|---|---|
| `app_user` | email UNIQUE, name, role, allowlisted | |
| `project` | title **NOT NULL**, status, tags `text[]`, owner; rest nullable | one required field, enforced in the DB |
| `project_update` | project_id, author, body, status_change, created_at | timeline source |
| `idea` | title **NOT NULL**, description, tags, author, promoted_project_id | |
| `slack_message` | channel_id, message_ts, slack_user, text, permalink, **UNIQUE(channel_id, message_ts)** | Slack retries become no-ops |
| `paper` | normalized_url UNIQUE, doi UNIQUE NULL, arxiv_id, title, authors `jsonb`, year, venue, `source`, `status` | one row per paper |
| `paper_share` | paper_id, slack_message_id, UNIQUE(both) | links papers to the messages that shared them |
| `paper_reference` | paper_id, ref_doi, ref_text | filled only when the registry has references |

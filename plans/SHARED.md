# Shared: phases, handoffs, schema

What both of you depend on. Individual tasks are in [PRIYAL.md](PRIYAL.md) and [SUJAL.md](SUJAL.md).

## Phases

| Phase | Works at the end |
|---|---|
| 0: Foundations | Skeleton builds, schema exists, allowlisted members can sign in, Slack app receives messages |
| 1: Core flows | A link posted in Slack shows up in the Papers tab; a member can create a project and post updates |
| 2: Complete features | Full paper resolution, search, ideas, auto-fill |
| 3: Stakeholder feedback | Projects and ideas have the lab's fields and built-in tags; each member has a profile, and Home shows new work that matches their interests; a People tab; a Search tab with filters |
| 4: Deploy | Runs on the lab's machine, installable from the README |

## Stakeholder feedback (round 1)

The lab's requests after the Phase 2 demo, and where each one is planned:

| # | Request | Owner | Tasks |
|---|---|---|---|
| 1 | More fields when adding a project or idea (status, date, core question, motivation, approach, data and resources, challenges, references, collaboration status, built-in and custom tags), plus archive papers as references, and upvotes and discussion on projects. Suggest (AI) fills most of them | Sujal | [SUJAL.md](SUJAL.md), Phase 3 |
| 2 | A profile for each member (intro, study background, fields of interest), shown on their Home page with their projects and ideas; new work tagged with their interests appears in their recent activity | Sujal | [SUJAL.md](SUJAL.md), Phase 3 |
| 3 | A People tab: name, background and fields of interest of every lab member | Priyal | [PRIYAL.md](PRIYAL.md), Phase 3 |
| 4 | Search as its own tab, with filters for date, tags, type and person | Priyal | [PRIYAL.md](PRIYAL.md), Phase 3 |
| 5 | A Lab Manager role: a lab member who can give and remove access, so the lab doesn't depend on us (or on the busy admin) after handover | Sujal | [SUJAL.md](SUJAL.md), Phase 3 |

Decisions that cut across both plans:

- **Tags stay as they are stored now** (`text[]`, trimmed, lowercased, no duplicates). A new `tag` table lists the lab's built-in tags with a display label and a group (Broad direction, Methodology, Other). A tag that isn't in the table is a custom tag and is still allowed. Projects, ideas, papers, profile interests and the search filter all use the same tags.
- Built-in tags (from the lab's list):
  - **Broad direction:** Bioinformatics, Drug Discovery, Genomics, Metabolic Engineering, Metabolic Modeling, Metagenomics, Synthetic Biology, Systems Biology, Theoretical Biology
  - **Methodology:** Algorithms, Artificial Intelligence, Big Data, Community Modeling, Constraint-based Modeling, Data Analysis, Deep Learning, Knowledge Graphs, Machine Learning, Metabolic Network Reconstruction, Network Analysis, Reinforcement Learning, Statistical Analysis, Systems Theory & Design Principles
  - **Other:** Molecular Computing, Open Source
- **Only the title is required** on a project or an idea; every new field is optional.
- **"References"** on a project or idea means two lists: external links (the existing `project_link`, plus a new `idea_link`) and papers from our own archive (`project_paper`, `idea_paper`).
- **Interest matches are worked out when Home is loaded**, not stored as notifications: anything from the last 30 days, by someone else, whose tags overlap the user's interests. No new notification table and no read/unread state for now.
- **Roles:** a new `app_user.role`: `MEMBER` (default), `LAB_MANAGER` or `ADMIN`.
  - **Admin** (the PI): Home shows lab-wide data (numbers and all recent activity) above their own profile.
  - **Lab Manager:** a normal member who can also add people, remove their access and change roles from a Manage access page. Admins can do this too.
  - **Member:** their own dashboard.
  - Nobody can remove their own access or change their own role, and the last admin can't be removed or demoted, so the lab can't lock itself out.
- Paper tags: papers from Slack have no tags. Papers get a `tags` column so the tag filter and interest matches cover them too. Tags are set in the paper's Details page, and the title is also matched against interests so untagged papers still show up.

Answered by the lab:

| Question | Answer |
|---|---|
| What should an admin see or do that a member can't? | Their Home shows lab-wide data. Managing access is the Lab Manager's job (admins can do it too) |
| Should "Active" and "Done" read "Ongoing" and "Completed"? | Yes, as labels only (`ProjectStatus.label`), no migration |
| Is "Date" the start date, or should there also be an end date? | Start date only (`started_on`), defaulting to the day the project is added |

Still open (the plan uses the default):

| Question | Default in the plan |
|---|---|
| Should a custom tag that many people use become a built-in tag? | A developer adds it later with a migration |

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
| H11 | Sujal → Priyal | Tag catalog: `tag` table, `GET /api/tags`, and the `tagPicker` widget in `static/js/tags.js` | paper tags, the search tag filter, the People interest filter |
| H12 | Sujal → Priyal | New project and idea text columns (core question, motivation, approach, resources, challenges) | rebuilding the `search` columns |
| H13 | Sujal → Priyal | Profile and role columns on `app_user` and the `ProfileView` record | People tab |
| H14 | Priyal → Sujal | `paper.tags` column and tag editing on the paper Details page | interest matches for papers on Home |
| H15 | Sujal → Priyal | `project_paper` / `idea_paper` tables | "Used in" list on the paper Details page |

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

Migration numbers are taken at merge time (next after the highest on `main`, V12 onwards).

| Table / columns | With |
|---|---|
| `tag`: name (lowercase, primary key), label, category (`DIRECTION`/`METHOD`/`OTHER`), position; seeded with the built-in tags above | Tag catalog (Sujal, H11) |
| `project` (added): started_on DATE, core_question, motivation, approach, resources, challenges (TEXT), open_for_collab BOOLEAN default false, collab_opened_at TIMESTAMPTZ | Project fields (Sujal, H12) |
| `idea` (added): core_question, motivation, approach, resources, challenges (TEXT), open_for_collab BOOLEAN default false | Idea fields (Sujal, H12) |
| `idea_link`: idea_id, label, url (http(s) only), created_at, like `project_link` | Idea references (Sujal) |
| `project_paper`, `idea_paper`: project_id / idea_id, paper_id (primary key on both), added_by, created_at | Archive papers as references (Sujal, H15) |
| `project_vote`: project_id, user_id (primary key on both), created_at, like `idea_vote` | Project upvotes (Sujal) |
| `project_comment`: project_id, author_id, body (not blank), created_at, like `idea_comment` | Project discussion (Sujal) |
| `app_user` (added): bio, background (TEXT), interests `text[]` default `'{}'`, role (`MEMBER`/`LAB_MANAGER`/`ADMIN`, default `MEMBER`); a migration allowlists the lab's admin and first Lab Manager | Profiles and roles (Sujal, H13) |
| `paper` (added): tags `text[]` default `'{}'` | Paper tags (Priyal, H14) |
| `slack_message` (added): poster_id → `app_user`, set when the Slack poster's email matches a user | Person filter for papers (Priyal) |
| `project`, `idea`, `paper` (changed): `search` dropped and re-added to include the new text fields and paper tags; GIN indexes recreated; GIN index on each `tags` column | Search filters (Priyal, after H12 and H14) |

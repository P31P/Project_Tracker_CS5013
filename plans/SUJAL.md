# Sujal: Modules D, E, F (+ half of G)

The foundation and the project side of the platform: auth + data layer (D), projects & ideas (E), and AI auto-fill (F). Priyal's modules build on your skeleton and schema, so do H1, H4 and H6 first. Shared phases, handoffs and schema: [SHARED.md](SHARED.md).

---

## 1. What you own

```
pom.xml, .github/workflows/ci.yml
src/main/java/tracker/
    TrackerApplication.java, config/, common/
    auth/       SecurityConfig, AllowlistOidcUserService, MeController, AccessController, AccessService
    data/       entity/, enums/, repository/
    projects/   ProjectController, IdeaController, ProjectService, IdeaService, dto/, TagController
    overview/   OverviewController, OverviewService (Priyal adds the profile Home and "For you" in Phase 3)
    llm/        LlmClient, HostedLlmClient, LocalLlmClient, DisabledLlmClient
    autofill/   AutoFillController, AutoFillService, AutoFillSuggestion
src/main/resources/
    application.yml, application-local.yml
    db/migration/V1__init.sql
    prompts/autofill.txt
    static/index.html, css/app.css, js/api.js, js/app.js, js/projects.js, js/ideas.js, js/autofill.js,
    js/home.js, js/tags.js, js/access.js
src/test/java/.../support/PostgresTestBase.java, auth/, data/, projects/, autofill/
scripts/seed-mock-data.sql
deploy/tracker.service, deploy/application-prod.yml.example, docs/INSTALL.md
```

---

## 2. Handoffs

See [SHARED.md](SHARED.md).

---

## 3. Tasks

### Phase 0: Foundations

- [x] **H1:** skeleton from `SCAFFOLDING.md` (scaffold dependencies only), `application.yml`, `ci.yml`
- [x] Local Postgres: `tracker` database and user; `application-local.yml`
- [x] Slack tables reviewed by Priyal (**H3**, in PR #6)
- [x] **H4:** migrations `V1__init.sql` (users, projects) and `V2__create_slack_tables.sql` (Slack messages, papers, shares), entities, repositories; `ConstraintsTest`, `SlackTablesTest`
- [x] **H6:** Google sign-in with an allowlist
  - Google Cloud OAuth client, redirect `http://localhost:8080/login/oauth2/code/google`
  - `SecurityConfig`: every page requires sign-in; `requireAllowlisted` refuses anyone not allowlisted
  - `AllowlistService`, `MeController` (`GET /api/me`)
  - `V3__allowlist_team.sql` allowlists both team members
  - `AllowlistServiceTest`, `SecurityConfigTest`, `MeControllerTest`

### Phase 1: Projects + dashboard

- [x] `ApiError` + `GlobalExceptionHandler` (adds validation, webmvc-test): validation errors come back as JSON naming the field
- [x] `ProjectController`/`ProjectService`: create (title only), list, get, `PATCH`; `POST /projects/{id}/updates`; `GET /projects/{id}/timeline` (newest first)
- [x] `ProjectApiTest`
- [x] **H7:** `index.html`, `app.css`, `api.js` (fetch wrapper; a 401 sends the user to sign-in), `app.js` (header, tabs, `registerTab`, sign-in state from `/api/me`)
- [x] `projects.js`: list with status; new-project form with **one field** + "More details" expander; project page with timeline and an "add update" box

### Phase 2: Ideas + auto-fill

- [x] `IdeaController`/`IdeaService`: log (title only), list, tag, `POST /ideas/{id}/promote` (creates a linked project)
- [x] `ideas.js`: quick-log box, list, "Promote to project" button
- [x] Project members, tags and links (`V7`); idea tags and upvotes (`V8`); `GET /api/people`
- [x] Home tab (`GET /api/overview`): counts, my projects, popular ideas, recent activity
- [x] Board view of projects (drag a card to change status); line icons, avatars, coloured tags
- [x] **H2/H8:** `LlmClient.completeJson(prompt, schema)`: one OpenAI-style client instead of separate hosted/local/disabled classes, since Ollama, Groq and Gemini accept the same API; `llm:` section in `application.yml` (`LLM_ENABLED`, base URL, model, API key, timeout). No retry: a local model answers in seconds or not at all
- [x] `AutoFillService`: `prompts/autofill.txt` → Jackson → `AutoFillSuggestion`; validate the title length and tags; drop unknown fields. Bad output gives "couldn't suggest", never a 500
- [x] `AutoFillController`: `POST /api/autofill` returns a suggestion and **never saves**
- [x] `autofill.js`: "Suggest" fills the form; the user edits and then saves
- [x] `AutoFillServiceTest`: short fragments, missing fields, special characters/Unicode, non-JSON reply, timeout
- [x] With `LLM_ENABLED=false` (the default), every form still works and the Suggest button is hidden
- [x] Local by default: Ollama with `llama3.2:3b` (about 6–8 s per suggestion on a laptop CPU); a hosted API is only a settings change if the lab allows it
- [ ] **H9:** `scripts/seed-mock-data.sql` (15–20 projects + ideas)
- [x] Review Priyal's search migration (**H10**)
- [ ] Page loads under 0.5 s on the seed data; add indexes where needed

### Phase 3: Stakeholder feedback (requests 1 and 2 in [SHARED.md](SHARED.md))

Do the tag catalog and the new columns first: Priyal's search filters, profiles and People tab wait on H11–H13.

**Tag catalog (→ H11)**

- [ ] Migration: `tag` table seeded with the lab's built-in tags (list in [SHARED.md](SHARED.md)), stored lowercase like every other tag, with a display `label`, `category` and `position`
- [ ] `Tag` entity + repository; `GET /api/tags`: the built-in tags in groups, plus custom tags already in use (from `project.tags`, `idea.tags`, and `app_user.interests`) so people reuse them
- [ ] `static/js/tags.js`: `tagPicker(selected)`, the built-in tags as toggle chips under their group headings (as in the lab's screenshots), a scrolling box, and an "Add a custom tag" input. Shared with Priyal for search filters, paper tags and the People filter
- [ ] `tagPill` shows the catalog label ("Machine Learning") for built-in tags and the stored text for custom ones
- [ ] `TagApiTest`: groups come back in order; a custom tag in use is listed once

**Projects: new fields (→ H12)**

- [ ] Migration: `project` gets `started_on`, `core_question`, `motivation`, `approach`, `resources`, `challenges`, `open_for_collab`, `collab_opened_at` (set when collaboration is switched on, cleared when off)
- [ ] `ProjectRequest`, `ProjectDetail`, `ProjectSummary` carry the new fields; all optional, title still the only required one; `started_on` defaults to today
- [ ] Status labels read Planning / Ongoing / On hold / Completed (labels only, no migration; confirmed by the lab)
- [ ] New project dialog: **Basics** (title, description, status, start date, collaboration "Open for collaboration / Closed or solo", tags with `tagPicker`, members) and a **Detailed info** expander (core question, motivation, proposed approach, data and resources needed, challenges). Long fields are Markdown
- [ ] **References** in the dialog and on the project page: external links (existing `project_link`) and papers from our archive. The paper picker uses `GET /api/search?type=PAPER&q=` (Priyal's endpoint, no new API)
- [ ] Migration: `project_paper`; `POST /api/projects/{id}/papers` (`paperId`), `DELETE /api/projects/{id}/papers/{paperId}` (→ H14)
- [ ] Project page: one section per filled detail field (empty ones hidden, an "Add" link for the owner and members), references, then timeline
- [ ] Upvotes: `project_vote` migration, `PUT`/`DELETE /api/projects/{id}/vote`, count and "voted" flag in the summary and detail
- [ ] Discussion: `project_comment` migration, `GET`/`POST /api/projects/{id}/comments`, a Discussion section on the project page (same component as ideas)
- [ ] List and board: an "Open for collaboration" badge and vote count on each card; a filter chip for open projects
- [ ] `ProjectApiTest`: create with only a title; create with every field; a link that isn't http(s) is refused; adding the same paper twice is a no-op; one vote per person; turning collaboration on sets `collab_opened_at`

**Ideas: the same fields where they fit**

- [ ] Migration: `idea` gets the five detail fields and `open_for_collab` (no status or date: an idea is open until it is promoted); `idea_link`, `idea_paper`
- [ ] Idea box: quick capture stays one line; "More details" opens description, tags (`tagPicker`), the detail fields, collaboration, links and archive papers
- [ ] `POST /api/ideas/{id}/links`, `DELETE /api/ideas/{id}/links/{linkId}`, `POST`/`DELETE /api/ideas/{id}/papers…`
- [ ] Promote to project copies the detail fields, collaboration, links and papers
- [ ] `IdeaApiTest`: the new fields round-trip; promotion copies them

**Auto-fill on the new fields**

- [ ] `AutoFillSuggestion` gains `coreQuestion`, `motivation`, `approach`, `resources`, `challenges` (each may be `null`)
- [ ] `prompts/autofill.txt`: send the built-in tag list and ask for tags from it first (a custom tag only if none fits); leave a field empty when the note doesn't say anything about it. Status, date, collaboration, members and references are never suggested
- [ ] `AutoFillService.read`: check each new field (length cap, blank → `null`); unknown built-in tags are kept as custom tags
- [ ] `autofill.js`: fills only empty fields and opens **Detailed info** when it filled something there; keeps typed tags (as now)
- [ ] `AutoFillServiceTest`: a note that only gives a title fills no detail fields; a full note fills all five; tags from the catalog come back with the catalog spelling
- [ ] Time a full suggestion on `llama3.2:3b`; if it is much slower than now, ask for the detail fields only when "Detailed info" is open

**Lab Manager: managing access (request 5)**

Today access is a migration, and `requireAllowlisted` checks it only at sign-in. After handover the Lab Manager does it from the app. Profiles and the personal Home are Priyal's (request 2); the `role` column is yours.

- [ ] Migration: `app_user.role` (`MEMBER`/`LAB_MANAGER`/`ADMIN`, default `MEMBER`); a second migration allowlists the lab's admin and first Lab Manager and sets their roles (emails from the lab, never guessed) (→ H13)
- [ ] `GET /api/me` returns `role`, so the frontend knows whether to show Manage access and the admin Home
- [ ] A helper beside `CurrentUser` answers "can manage access" (`LAB_MANAGER` or `ADMIN`); the access endpoints give 403 "Only a Lab Manager can manage access" to anyone else
- [ ] `tracker.auth.AccessController` + `AccessService`:
  - `GET /api/access`: everyone with an `app_user` row (name, email, role, whether they have access)
  - `POST /api/access` (`email` required; `name`, `role` optional): gives the email access, creating the row if needed; the email is trimmed and lowercased
  - `PATCH /api/access/{id}` (`role` and/or `allowlisted`): change someone's role, or remove or restore their access
- [ ] Removing access sets `allowlisted = false` and never deletes the row, so their projects, ideas and comments keep their name
- [ ] Safety rules: you can't change your own role or remove yourself; the last `ADMIN` can't be removed or demoted; only an `ADMIN` can make someone an `ADMIN`
- [ ] Access is checked on every request, not only at sign-in: someone whose access is removed gets 401 on their next API call and is sent to the sign-in page
- [ ] Frontend: a **Manage access** item in the account menu, shown only to Lab Managers and admins. It opens `#access` (`static/js/access.js`, not in the tab bar): a table of people with their role and access, an "Add person" dialog (email, name, role), a role select per row, and Remove / Restore access with a confirmation
- [ ] Sign-in page: someone without access is told to ask the Lab Manager, instead of a bare error
- [ ] `AccessApiTest`: a member gets 403; a Lab Manager can add, remove and restore; an added email passes `requireAllowlisted`; a removed person's next API call gives 401; you can't remove yourself; the last admin can't be demoted; a Lab Manager can't create an admin
- [ ] Once this lands, update the CLAUDE.md convention "add people with a new migration": people are added from Manage access, and migrations are only for the first admin and Lab Manager

**Seed data**

- [ ] **H9** now also fills the new fields, profiles, interests and some paper tags, so the People tab and filters have something to show

### Phase 4: Deploy (your half of G)

- [ ] Packaging (adds actuator): one jar via `spring-boot-maven-plugin`; external config via `--spring.config.additional-location`
- [ ] `deploy/application-prod.yml.example`, `deploy/tracker.service` (systemd, restart on failure)
- [ ] Postgres on the deployment machine; OAuth redirect URI for the production host
- [ ] Hand over the Google OAuth client so the Lab Manager never needs the Google Cloud console: the client is owned by a lab account, not ours, and its consent screen is **In production** (or **Internal**, if the lab's Google Workspace allows it), so sign-in isn't limited to the console's test-user list. `docs/INSTALL.md` says how
- [ ] `docs/INSTALL.md`: from a bare machine to a running app
- [ ] README: quick start + configuration reference

---

## 4. Done when

| Module | Done when |
|---|---|
| D | Non-allowlisted accounts are refused; a project saves with only a title |
| E | Pages load in under 0.5 s on the seed data; the timeline shows updates in order; ideas can be promoted |
| F | Messy inputs give valid suggestions or "couldn't suggest"; with the LLM off every form still works; nothing is saved without confirmation |
| Feedback 1 | A project or idea saves with only a title or with every field; built-in and custom tags both work; links and archive papers can be added and removed; projects can be upvoted and discussed; Suggest fills the detail fields and prefers built-in tags |
| Feedback 5 | A Lab Manager adds a member by email from the app and that person can sign in; removing someone cuts them off on their next request; members can't reach Manage access; nobody can lock the lab out |

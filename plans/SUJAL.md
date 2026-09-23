# Sujal: Modules D, E, F (+ half of G)

The foundation and the project side of the platform: auth + data layer (D), projects & ideas (E), and AI auto-fill (F). Priyal's modules build on your skeleton and schema, so do H1, H2, H4 and H6 first. Overall plan: [../PLAN.md](../PLAN.md).

Branch prefixes: `d/…`, `e/…`, `f/…`, `g/…`

---

## 1. What you own

```
pom.xml, .env.example, .github/workflows/ci.yml
src/main/java/in/ac/iitm/ramanlab/tracker/
    TrackerApplication.java, config/, common/
    auth/       SecurityConfig, AllowlistOidcUserService, MeController
    data/       entity/, enums/, repository/
    projects/   ProjectController, IdeaController, ProjectService, IdeaService, dto/
    llm/        LlmClient, HostedLlmClient, LocalLlmClient, DisabledLlmClient
    autofill/   AutoFillController, AutoFillService, AutoFillSuggestion
src/main/resources/
    application.yml, application-local.yml
    db/migration/V1__init.sql
    prompts/autofill.txt
    static/index.html, css/app.css, js/api.js, js/app.js, js/projects.js, js/ideas.js, js/autofill.js
src/test/java/.../support/PostgresTestBase.java, auth/, data/, projects/, autofill/
scripts/seed-mock-data.sql
deploy/tracker.service, deploy/application-prod.yml.example, docs/INSTALL.md
```

---

## 2. Handoffs

This table is the same in both plans.

| # | From → to | What | Needed before |
|---|---|---|---|
| H1 | Sujal → Priyal | Maven skeleton (`SCAFFOLDING.md`), CI | any code |
| H2 | Sujal → Priyal | `LlmClient` + `DisabledLlmClient` | rung 3 |
| H3 | Priyal → Sujal | Review of `slack_message`, `paper`, `paper_share`, `paper_reference` in the V1 draft | H4 |
| H4 | Sujal → Priyal | `V1__init.sql`, entities, repositories, `PostgresTestBase` | ingestion, worker |
| H5 | Priyal → Sujal | Slack app tokens + test-channel access (outside git) | running the full flow locally |
| H6 | Sujal → Priyal | `SecurityConfig` + test helper for signed-in MockMvc | `PaperController`, `SearchController` |
| H7 | Sujal → Priyal | Dashboard shell: `index.html`, `api.js`, `app.js` with `registerTab(id, label, render)` | `papers.js`, `search.js` |
| H8 | Sujal → Priyal | `HostedLlmClient` | running rung 3 for real |
| H9 | Sujal → Priyal | `scripts/seed-mock-data.sql` | `SearchServiceTest` |
| H10 | Priyal → Sujal | Search migration (touches `project` and `idea`) | search |

---

## 3. Tasks

### Phase 0: Foundations

- [ ] **H1:** skeleton from `SCAFFOLDING.md` (scaffold dependencies only), `application.yml`, `.env.example`, `ci.yml`
- [ ] **H2:** `LlmClient` (`String complete(String prompt)`) + `DisabledLlmClient`, selected by `llm.provider = hosted | local | disabled`
- [ ] Local Postgres: `tracker` database and user; `application-local.yml`
- [ ] V1 draft for Priyal's review (**H3**)
- [ ] **H4:** `V1__init.sql` (schema in PLAN.md §2.2), entities, enums, repositories, `PostgresTestBase`; `ConstraintsTest` (a project saves with only a title; a duplicate `(channel_id, message_ts)` is rejected)
- [ ] **H6** (adds security, OAuth2 client, security-test):
  - Google OAuth client, redirect `http://localhost:8080/login/oauth2/code/google`
  - `SecurityConfig`: `/api/**` requires sign-in, static files are public
  - `AllowlistOidcUserService`, `MeController`
  - allowlist seeded with both team members
  - `AllowlistTest`; signed-in MockMvc helper

### Phase 1: Projects + dashboard

- [ ] `ApiError` + `GlobalExceptionHandler` (adds validation, webmvc-test): validation errors come back as JSON naming the field
- [ ] `ProjectController`/`ProjectService`: create (title only), list, get, `PATCH`; `POST /projects/{id}/updates`; `GET /projects/{id}/timeline` (newest first)
- [ ] `ProjectApiTest`
- [ ] **H7:** `index.html`, `app.css`, `api.js` (fetch wrapper; a 401 sends the user to sign-in), `app.js` (header, tabs, `registerTab`, sign-in state from `/api/me`)
- [ ] `projects.js`: list with status; new-project form with **one field** + "More details" expander; project page with timeline and an "add update" box

### Phase 2: Ideas + auto-fill

- [ ] `IdeaController`/`IdeaService`: log (title only), list, tag, `POST /ideas/{id}/promote` (creates a linked project)
- [ ] `ideas.js`: quick-log box, list, "Promote to project" button
- [ ] **H8:** `HostedLlmClient` over Java `HttpClient`: timeout, API key from env, one retry
- [ ] `AutoFillService`: `prompts/autofill.txt` → Jackson → `AutoFillSuggestion`; validate the title length and tags; drop unknown fields. Bad output gives "couldn't suggest", never a 500
- [ ] `AutoFillController`: `POST /api/autofill` returns a suggestion and **never saves**
- [ ] `autofill.js`: "Suggest" fills the form; the user edits and then saves
- [ ] `AutoFillServiceTest`: short fragments, missing fields, special characters/Unicode, non-JSON reply, timeout
- [ ] With `llm.provider=disabled`, every form still works and the Suggest button is hidden
- [ ] If a commercial LLM isn't allowed: `LocalLlmClient` (OpenAI-compatible local endpoint, e.g. Ollama)
- [ ] **H9:** `scripts/seed-mock-data.sql` (15–20 projects + ideas)
- [ ] Review Priyal's search migration (**H10**)
- [ ] Page loads under 0.5 s on the seed data; add indexes where needed

### Phase 3: Deploy (your half of G)

- [ ] Packaging (adds actuator): one jar via `spring-boot-maven-plugin`; external config via `--spring.config.additional-location`
- [ ] `deploy/application-prod.yml.example`, `deploy/tracker.service` (systemd, restart on failure)
- [ ] Postgres on the deployment machine; OAuth redirect URI for the production host
- [ ] `docs/INSTALL.md`: from a bare machine to a running app
- [ ] README: quick start + configuration reference

---

## 4. Done when

| Module | Done when |
|---|---|
| D | Non-allowlisted accounts are refused; a project saves with only a title |
| E | Pages load in under 0.5 s on the seed data; the timeline shows updates in order; ideas can be promoted |
| F | Messy inputs give valid suggestions or "couldn't suggest"; with the LLM off every form still works; nothing is saved without confirmation |

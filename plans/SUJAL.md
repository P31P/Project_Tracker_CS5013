# Sujal: Modules D, E, F (+ half of G)

The foundation and the project side of the platform: auth + data layer (D), projects & ideas (E), and AI auto-fill (F). Priyal's modules build on your skeleton and schema, so do H1, H4 and H6 first. Shared phases, handoffs and schema: [SHARED.md](SHARED.md).

---

## 1. What you own

```
pom.xml, .github/workflows/ci.yml
src/main/java/tracker/
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

See [SHARED.md](SHARED.md).

---

## 3. Tasks

### Phase 0: Foundations

- [x] **H1:** skeleton from `SCAFFOLDING.md` (scaffold dependencies only), `application.yml`, `ci.yml`
- [x] Local Postgres: `tracker` database and user; `application-local.yml`
- [ ] Slack tables reviewed by Priyal (**H3**, in PR #6)
- [x] **H4:** migrations `V1__init.sql` (users, projects) and `V2__create_slack_tables.sql` (Slack messages, papers, shares), entities, repositories; `ConstraintsTest`, `SlackTablesTest`
- [x] **H6:** Google sign-in with an allowlist
  - Google Cloud OAuth client, redirect `http://localhost:8080/login/oauth2/code/google`
  - `SecurityConfig`: every page requires sign-in; `requireAllowlisted` refuses anyone not allowlisted
  - `AllowlistService`, `MeController` (`GET /api/me`)
  - `V3__allowlist_team.sql` allowlists both team members
  - `AllowlistServiceTest`, `SecurityConfigTest`, `MeControllerTest`

### Phase 1: Projects + dashboard

- [ ] `ApiError` + `GlobalExceptionHandler` (adds validation, webmvc-test): validation errors come back as JSON naming the field
- [ ] `ProjectController`/`ProjectService`: create (title only), list, get, `PATCH`; `POST /projects/{id}/updates`; `GET /projects/{id}/timeline` (newest first)
- [ ] `ProjectApiTest`
- [ ] **H7:** `index.html`, `app.css`, `api.js` (fetch wrapper; a 401 sends the user to sign-in), `app.js` (header, tabs, `registerTab`, sign-in state from `/api/me`)
- [ ] `projects.js`: list with status; new-project form with **one field** + "More details" expander; project page with timeline and an "add update" box

### Phase 2: Ideas + auto-fill

- [ ] `IdeaController`/`IdeaService`: log (title only), list, tag, `POST /ideas/{id}/promote` (creates a linked project)
- [ ] `ideas.js`: quick-log box, list, "Promote to project" button
- [ ] **H2:** `LlmClient` (`String complete(String prompt)`) + `DisabledLlmClient`, selected by `llm.provider = hosted | local | disabled`; add the `llm:` section to `application.yml`
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

# Workflow Plan: Unified Lab Project & Idea Tracking Platform

Priyal Patel owns Modules A, B, C · Sujal Jayant Kumar owns D, E, F · Both own G

- Personal plans: [plans/PRIYAL.md](plans/PRIYAL.md), [plans/SUJAL.md](plans/SUJAL.md). They share handoffs H1–H10.
- Project skeleton: [SCAFFOLDING.md](SCAFFOLDING.md) (handoffs H1 + H2).

---

## 1. How the system works (the two end-to-end flows)

**Flow 1: Paper link in Slack → Papers tab (no forms)**

```
Slack message with link
  → [A] receive event (Socket Mode or HTTP), verify, ack within 3 s
  → [A] save the Slack message (UNIQUE channel_id + message_ts → retries are no-ops)
  → [A] extract links (Slack wraps them as <url|label>), drop non-paper links
  → [A] create paper row(s) with status PENDING, attach permalink + poster
  → [B] async worker picks up PENDING papers, runs the resolution ladder:
        1. DOI / arXiv ID found → Crossref / arXiv API          source = REGISTRY
        2. else fetch page → citation_* meta tags               source = META_TAGS
        3. else bare PDF → text → LLM extracts title/authors    source = MODEL
        4. nothing worked                                       status = UNRESOLVED
  → [C] paper shows in Papers tab + search; MODEL/UNRESOLVED ones are flagged so users can fix them
```

**Flow 2: Member logs a project / idea / update**

```
Google sign-in → [D] allowlist check → dashboard
  → [E] create project/idea (only title required) or post an update to a timeline
  → [F] optional: paste free text → "Suggest" → LLM returns JSON → validated →
        shown as an editable suggestion → saved only when the user confirms
  → [C] everything is searchable from one search bar, filterable by type
```

---

## 2. Foundations (these unblock everything else)

### 2.1 Repo tree

Maven + Spring Boot only. There is no Docker and no Node build step. The only thing installed outside Java is PostgreSQL. Letters in brackets name the owning module.

```
Project_Tracker_CS5013/
├── pom.xml                              # grows as each task adds a library
├── README.md                            (G) what it is + quick start
├── PLAN.md                              this file
├── requirements.txt                     what to install, stage by stage
├── SCAFFOLDING.md                       how to create the skeleton (Phase 0)
├── plans/
│   ├── PRIYAL.md                        A, B, C checklist
│   └── SUJAL.md                         D, E, F checklist
├── Proposal_Doc.pdf
├── Design_Doc.pdf
├── .gitignore                           # target/, .env, IDE folders
├── .env.example                         # every env var the app reads, with no values
├── .github/
│   └── workflows/
│       └── ci.yml                       # mvn verify on every PR
├── docs/                                (G)
│   ├── INSTALL.md                       # install Postgres, set env vars, run the jar
│   └── ADMIN.md                         # add a lab member, add a Slack channel, fix a paper
├── deploy/                              (G)
│   ├── tracker.service                  # systemd unit that keeps the jar running
│   ├── application-prod.yml.example     # config kept outside the jar
│   └── smoke-check.sh                   # health, sign-in page, 30-min idle check
├── scripts/
│   └── seed-mock-data.sql               # the 15–20 mock projects for demos and tests
└── src/
    ├── main/
    │   ├── java/in/ac/iitm/ramanlab/tracker/
    │   │   ├── TrackerApplication.java
    │   │   ├── config/                  (D) shared beans: AsyncConfig, HttpClientConfig, JacksonConfig
    │   │   ├── common/                  (D) ApiError, GlobalExceptionHandler
    │   │   ├── auth/                    (D)
    │   │   │   ├── SecurityConfig.java           # Google OAuth2 login, protects /api/**
    │   │   │   ├── AllowlistOidcUserService.java # refuses emails not on the allowlist
    │   │   │   └── MeController.java             # GET /api/me
    │   │   ├── data/                    (D) the layer every module depends on
    │   │   │   ├── entity/              # AppUser, Project, ProjectUpdate, Idea,
    │   │   │   │                        # SlackMessage, Paper, PaperShare, PaperReference
    │   │   │   ├── enums/               # ProjectStatus, PaperSource, PaperStatus
    │   │   │   └── repository/          # one Spring Data repository per entity
    │   │   ├── projects/                (E)
    │   │   │   ├── ProjectController.java        # projects, updates, timeline
    │   │   │   ├── IdeaController.java           # ideas, promote to project
    │   │   │   ├── ProjectService.java
    │   │   │   ├── IdeaService.java
    │   │   │   └── dto/                 # request/response records, TimelineEntry
    │   │   ├── llm/                     (F) shared by F and B's PDF rung
    │   │   │   ├── LlmClient.java                # interface
    │   │   │   ├── HostedLlmClient.java          # commercial API over Java HttpClient
    │   │   │   ├── LocalLlmClient.java           # local open-source model (plan B)
    │   │   │   └── DisabledLlmClient.java        # LLM switched off
    │   │   ├── autofill/                (F)
    │   │   │   ├── AutoFillController.java       # POST /api/autofill
    │   │   │   ├── AutoFillService.java          # prompt → JSON → validate
    │   │   │   └── AutoFillSuggestion.java
    │   │   ├── slack/                   (A)
    │   │   │   ├── SlackAppConfig.java           # Bolt App, Socket Mode
    │   │   │   ├── MessageEventHandler.java      # ack fast, hand off async
    │   │   │   ├── IngestionService.java         # save message, create PENDING papers
    │   │   │   ├── LinkExtractor.java            # <url|label>, DOI, arXiv, PDF, publisher URLs
    │   │   │   └── SlackPoller.java              # plan B, off unless enabled in config
    │   │   ├── papers/                  (B)
    │   │   │   ├── PaperController.java          # list, detail, manual correction
    │   │   │   ├── PaperResolutionWorker.java    # @Scheduled, claims PENDING rows
    │   │   │   ├── ResolutionLadder.java         # tries resolvers in order
    │   │   │   ├── resolver/
    │   │   │   │   ├── PaperResolver.java        # interface
    │   │   │   │   ├── CrossrefResolver.java     # rung 1
    │   │   │   │   ├── ArxivResolver.java        # rung 1
    │   │   │   │   ├── MetaTagResolver.java      # rung 2
    │   │   │   │   └── PdfLlmResolver.java       # rung 3
    │   │   │   └── dto/
    │   │   └── search/                  (C)
    │   │       ├── SearchController.java         # GET /api/search
    │   │       ├── SearchService.java            # tsvector queries across three tables
    │   │       └── SearchResult.java
    │   └── resources/
    │       ├── application.yml                   # defaults; secrets come from env vars
    │       ├── application-local.yml             # local Postgres, Slack test channel
    │       ├── db/migration/            (D reviews all)
    │       │   ├── V1__init.sql                  # schema v1 from 2.2
    │       │   └── V20261017__search_indexes.sql # tsvector columns + GIN indexes (C)
    │       ├── prompts/
    │       │   ├── autofill.txt                  (F)
    │       │   └── pdf-metadata.txt              (B)
    │       └── static/                           # dashboard, served by Spring Boot → one jar
    │           ├── index.html
    │           ├── css/app.css
    │           └── js/
    │               ├── api.js                    # fetch wrapper, error handling
    │               ├── app.js                    # tabs, routing, sign-in state
    │               ├── projects.js               (E) list, form, timeline
    │               ├── ideas.js                  (E)
    │               ├── autofill.js               (F) "Suggest" button
    │               ├── papers.js                 (C) Papers tab, "Fix details"
    │               └── search.js                 (C)
    └── test/
        ├── java/in/ac/iitm/ramanlab/tracker/
        │   ├── support/
        │   │   └── PostgresTestBase.java         # starts embedded Postgres (Zonky), runs Flyway
        │   ├── auth/AllowlistTest.java
        │   ├── data/ConstraintsTest.java         # title-only project, duplicate Slack event
        │   ├── projects/ProjectApiTest.java
        │   ├── autofill/AutoFillServiceTest.java
        │   ├── slack/LinkExtractorTest.java
        │   ├── slack/IngestionReplayTest.java
        │   ├── papers/ResolutionLadderTest.java  # WireMock serves recorded responses
        │   └── search/SearchServiceTest.java
        └── resources/
            ├── application-test.yml
            └── fixtures/
                ├── slack/                        # messages from the lab export
                ├── crossref/                     # recorded API responses
                ├── arxiv/
                ├── html/                         # publisher pages with citation_* tags
                ├── pdf/
                └── autofill/                     # synthetic messy inputs
```

The frontend is plain JS, with no framework or build step, so no Node install is needed. It lives in `static/` and ships inside the jar, which keeps deployment to **one jar**, as Module G promises. Tests run against a real Postgres started from Java (Zonky embedded Postgres), not H2, so constraints and full-text search behave the same as in production.

### 2.2 Schema v1 (Sujal writes the migration, Priyal reviews)

| Table | Key columns | Notes |
|---|---|---|
| `app_user` | email UNIQUE, name, role, allowlisted | the allowlist is a flag or a separate table |
| `project` | title **NOT NULL**, everything else nullable, status, tags `text[]`, owner | "one required field" is enforced in the DB |
| `project_update` | project_id, author, body, status_change, created_at | the timeline is built from this table |
| `idea` | title **NOT NULL**, description, tags, author, promoted_project_id | an idea can later become a project |
| `slack_message` | channel_id, message_ts, slack_user, text, permalink, **UNIQUE(channel_id, message_ts)** | stops duplicates when Slack retries |
| `paper` | normalized_url UNIQUE, doi UNIQUE NULL, arxiv_id, title, authors `jsonb`, year, venue, `source` (REGISTRY/META_TAGS/MODEL/MANUAL), `status` (PENDING/RESOLVED/UNRESOLVED) | one row per paper even if it's shared many times |
| `paper_share` | paper_id, slack_message_id, UNIQUE(both) | one message can hold several links; one paper can be shared several times |
| `paper_reference` | paper_id, ref_doi, ref_text | only filled when the registry has the references |

Full-text search uses a generated `tsvector` column plus a GIN index on `project`, `idea` and `paper`.

### 2.3 Interfaces between modules (agree on them, then build in parallel)
- `LlmClient` (owned by F, used by B and F): `String complete(String prompt)`, where the caller parses the reply. Implementations: hosted API, local model (Ollama-style endpoint) and `Disabled`, chosen by config.
- `PaperQueue` (A → B): A inserts `paper` rows with status PENDING, and B's `@Scheduled` worker claims them (`FOR UPDATE SKIP LOCKED`). The Postgres table is the queue, so we need no broker.
- The REST contract below is fixed before the frontend is written.

### 2.4 REST API v1
```
GET    /api/me
GET    /api/projects            POST /api/projects
GET    /api/projects/{id}       PATCH /api/projects/{id}
GET    /api/projects/{id}/timeline
POST   /api/projects/{id}/updates
GET    /api/ideas               POST /api/ideas     POST /api/ideas/{id}/promote
GET    /api/papers?status=      GET /api/papers/{id}  PATCH /api/papers/{id}   (manual correction → source=MANUAL)
GET    /api/search?q=&type=project,idea,paper
POST   /api/autofill            {text} → {title, tags, description} suggestion only, never saved
POST   /slack/events            (only if HTTP mode; not needed with Socket Mode)
GET    /actuator/health
```

---

## 3. Phases

Each phase ends with something that works end to end.

### Phase 0: Foundations

| Owner | Task |
|---|---|
| Sujal (D) | Skeleton from `SCAFFOLDING.md`; `V1__init.sql` (schema above), entities, repositories, `PostgresTestBase` |
| Sujal (D) | Google OAuth2 sign-in, allowlist check, `/api/me` |
| Priyal (A) | Slack app with Socket Mode, scopes `channels:history` + `channels:read`, a test channel |
| Priyal (A) | `LinkExtractor` + tests: Slack `<url\|label>`, arXiv abs/pdf, `doi.org`, DOIs in publisher URLs, IEEE document URLs, plain PDFs; non-paper links ignored |
| Both | CI green; branch protection on `main` |

**Works at the end:** `./mvnw verify` passes, and allowlisted accounts can sign in.

### Phase 1: Core flows

| Owner | Task |
|---|---|
| Priyal (A) | Bolt `message` handler → save `slack_message` → extract links → PENDING `paper` + `paper_share`; permalink via `chat.getPermalink`; ack fast, process async; replay test |
| Priyal (B) | Rung 1: DOI → Crossref, arXiv ID → arXiv API; resolution worker |
| Priyal (C) | Papers tab: read-only list |
| Sujal (E) | Project CRUD, updates, timeline; only the title is required; validation errors as JSON |
| Sujal (E) | Dashboard shell (`registerTab`), project list, one-field form, timeline page |

**Works at the end:** a link posted in the test channel shows up in the Papers tab with its title. A signed-in member can create a project with just a title, post an update and see the timeline.

### Phase 2: Complete features

| Owner | Task |
|---|---|
| Priyal (B) | Rung 2 meta tags, rung 3 PDF → `LlmClient`; references from Crossref; retry/backoff; UNRESOLVED path; WireMock tests |
| Priyal (A) | If Slack events aren't arriving reliably: `SlackPoller` (`conversations.history` on a schedule, feeding the same pipeline) |
| Priyal (C) | Search migration, `/api/search` with a type filter, search UI; paper detail with a "Fix details" form |
| Sujal (E) | Ideas: log, list, tag, promote to project; `seed-mock-data.sql` |
| Sujal (F) | `HostedLlmClient`; auto-fill service + "Suggest" button; messy-input tests; everything works with the LLM disabled |
| Sujal (F) | If a commercial LLM isn't allowed: `LocalLlmClient` |
| Sujal (D/E) | Page loads under 0.5 s on the seed data; add indexes where needed |

**Works at the end:** every row in §6 except G.

### Phase 3: Deploy & hand off

| Owner | Task |
|---|---|
| Sujal (G) | Single jar with config outside it; systemd unit; Postgres on the deployment machine; OAuth redirect for the production host; `docs/INSTALL.md` |
| Priyal (G) | `smoke-check.sh`; point the Slack app at the lab's channels; `docs/ADMIN.md` |
| Both | README; a fresh install from the README alone works |

Future scope: speech-to-text on the description field, feeding the transcript into the same Module F pipeline.

---

## 4. Dependencies

```
D schema + repos ──► A ingestion ──► B ladder ──► C paper views/search
        │                                ▲
        └──► E projects/ideas ──► F auto-fill (owns LlmClient) ─┘ (B's rung 3 uses it)
All ──► G deploy
```

Socket Mode connects out to Slack over a WebSocket, so the app needs no public URL, both locally and on the lab network.

---

## 5. How we work

- **Branches:** `main` is protected and always runnable. Feature branches are named `a/slack-events`, `d/oauth-allowlist`, etc. PRs are small, and the other person reviews each one.
- **Migrations:** never edit a merged Flyway file. After `V1__init.sql`, use date versions (`V20261017__search_indexes.sql`) so two migrations never share a number.
- **Dependencies:** add a library in the PR that needs it; update `pom.xml` and `requirements.txt` together.
- **Secrets:** Slack tokens, Google client ID/secret, LLM key and DB credentials live only in the git-ignored `.env`.
- **Tests:** database tests run on embedded Postgres (a real Postgres, not H2). External APIs are tested with recorded responses via WireMock. CI must be green to merge.

---

## 6. Definition of done

| Module | Done when |
|---|---|
| A | Every fixture link format is captured; chat without links is ignored; a replayed event creates no duplicate |
| B | A known DOI gives exact registry data; a bare PDF gives `source=MODEL`; a dead link gives UNRESOLVED shown in the UI, never silently dropped |
| C | One keyword in a project title, an idea and a paper returns all three; the type filter works |
| D | Non-allowlisted Google accounts are refused; a project saves with only a title |
| E | Pages load in under 0.5 s on the seed data; the timeline shows updates in order |
| F | Messy inputs give valid suggestions or a clean "couldn't suggest"; with the LLM disconnected every form still works |
| G | The smoke check passes on the deployment machine; a fresh install works from the README alone |

# Priyal: Modules A, B, C (+ half of G)

The paper side of the platform: Slack ingestion (A), the paper archive and resolution ladder (B), and search + paper views (C). Overall plan: [../PLAN.md](../PLAN.md).

Branch prefixes: `a/…`, `b/…`, `c/…`, `g/…`

---

## 1. What you own

```
src/main/java/in/ac/iitm/ramanlab/tracker/
    slack/      SlackAppConfig, MessageEventHandler, IngestionService, LinkExtractor, SlackPoller
    papers/     PaperController, PaperResolutionWorker, ResolutionLadder, resolver/*, dto/
    search/     SearchController, SearchService, SearchResult
src/main/resources/
    prompts/pdf-metadata.txt
    db/migration/V2026MMDD__*.sql
    static/js/papers.js, static/js/search.js
src/test/java/.../slack/, papers/, search/
src/test/resources/fixtures/slack/, crossref/, arxiv/, html/, pdf/
deploy/smoke-check.sh, docs/ADMIN.md
```

Shared files: `pom.xml` + `requirements.txt` (add a library in the PR that needs it), `app.js` (only your `registerTab(...)` lines), `application.yml` (only the `slack:` and `papers:` sections).

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

**Work that doesn't need any handoff:** `LinkExtractor`, the Slack app setup, and the Crossref/arXiv resolvers. A resolver only turns an HTTP response into a `ResolvedMetadata` record, so WireMock tests are enough. Test rung 3 with a fake `LlmClient` until H8 lands.

---

## 3. Tasks

### Phase 0: Foundations

- [ ] Slack app: Socket Mode on, app-level token (`connections:write`), bot scopes `channels:history` + `channels:read`, subscribed to `message.channels`; installed in the workspace; a `#tracker-test` channel → **H5**
- [ ] Review the paper/slack tables in the V1 draft → **H3**
- [ ] `LinkExtractor` + `LinkExtractorTest`:
  - Slack `<url|label>` and `<url>` forms
  - `arxiv.org/abs/…` and `/pdf/…`, with and without a version suffix
  - `doi.org/…`, DOIs inside Springer/ACM/Wiley URLs, IEEE `document/<id>` URLs
  - plain `.pdf` links
  - GitHub, YouTube, Google Docs and other non-paper links are ignored
- [ ] URL normalisation (lowercase host, strip tracking params, strip arXiv version, `http`→`https`). The result is stored in `paper.normalized_url`
- [ ] Fixtures: messages from the test channel, recorded Crossref/arXiv responses

### Phase 1: Slack → database → Papers tab

- [ ] `SlackAppConfig` (adds Bolt Jakarta Socket Mode + Tyrus): `App` + `SocketModeApp` started and stopped with Spring; tokens from env
- [ ] `MessageEventHandler`: skip bot messages and edit/delete subtypes, ack right away, hand off to `IngestionService` via `@Async`
- [ ] `IngestionService`:
  - insert `slack_message` with `ON CONFLICT (channel_id, message_ts) DO NOTHING`; if the row already existed, stop (it's a retry)
  - permalink via `chat.getPermalink`
  - for each link: upsert `paper` by `normalized_url` (PENDING), insert `paper_share`
- [ ] `IngestionReplayTest`: the same event twice gives one message, one paper and one share
- [ ] `PaperResolver` + `CrossrefResolver` + `ArxivResolver` (rung 1, adds WireMock for tests)
- [ ] `PaperResolutionWorker`: `@Scheduled` every 30 s, claims PENDING rows with `FOR UPDATE SKIP LOCKED`, runs the ladder, sets `source`/`status`
- [ ] `PaperController`: `GET /api/papers?status=`, `GET /api/papers/{id}`
- [ ] `papers.js`: read-only list with title, authors, poster, date, Slack permalink, and a badge for MODEL/UNRESOLVED

### Phase 2: Full ladder + search

- [ ] If Slack events aren't arriving reliably, build `SlackPoller`: `@Scheduled` `conversations.history` with `oldest=<last ts>` per channel, a cursor table, feeding the same `IngestionService`. It's turned on by a config flag
- [ ] `MetaTagResolver` (rung 2, adds Jsoup): real User-Agent, 10 s timeout, follow redirects; read `citation_title`, `citation_author`, `citation_doi`, `citation_publication_date`; if a DOI is found, send it back through rung 1
- [ ] `PdfLlmResolver` (rung 3, adds PDFBox): size cap (~20 MB), text from the first 2 pages, `LlmClient` + `prompts/pdf-metadata.txt`, `source = MODEL`
- [ ] References: store Crossref's `reference` array in `paper_reference` when present; otherwise the UI says "references not available"
- [ ] Retry: an `attempts` column; after 3 failures the paper is marked UNRESOLVED. Nothing is ever deleted
- [ ] `ResolutionLadderTest`: known DOI → exact registry data; meta-tag page → `META_TAGS`; bare PDF → `MODEL`; dead link → `UNRESOLVED`
- [ ] Search migration: generated `tsvector` columns + GIN indexes on `project`, `idea`, `paper` → **H10**
- [ ] `SearchService`: `websearch_to_tsquery`, `ts_rank`, `UNION ALL` across the three tables, `type` filter, top 50
- [ ] `SearchController`: `GET /api/search?q=&type=`
- [ ] `search.js`: search bar in the header, results grouped by type, filter chips
- [ ] Paper detail page + "Fix details" form → `PATCH /api/papers/{id}`, which sets `source = MANUAL`
- [ ] `SearchServiceTest` on the seed data: one keyword across a project title, an idea and a paper returns all three

### Phase 3: Deploy (your half of G)

- [ ] Point the Slack app at the lab's channels; document the scopes and tokens in `docs/ADMIN.md`
- [ ] `deploy/smoke-check.sh`: health endpoint, sign-in page, and a page requested after 30 idle minutes comes back quickly
- [ ] `docs/ADMIN.md`: add a Slack channel, fix an UNRESOLVED paper, turn on the poller
- [ ] README: Slack app setup from scratch

---

## 4. Done when

| Module | Done when |
|---|---|
| A | Every fixture link format is captured; chat without links is ignored; a replayed event creates no duplicate |
| B | A known DOI gives exact registry data; a bare PDF gives `source=MODEL`; a dead link gives UNRESOLVED shown in the UI |
| C | One keyword across a project, an idea and a paper returns all three; the type filter works; MODEL/UNRESOLVED papers can be fixed from the UI |

# Priyal: Modules A, B, C (+ half of G)

The paper side of the platform: Slack ingestion (A), the paper archive and resolution ladder (B), and search + paper views (C). Shared phases, handoffs and schema: [SHARED.md](SHARED.md).

---

## 1. What you own

```
src/main/java/tracker/
    slack/      SlackAppConfig, MessageEventHandler, IngestionService, LinkExtractor, SlackPoller
    papers/     PaperController, PaperResolutionWorker, ResolutionLadder, resolver/*, dto/
    search/     SearchController, SearchService, SearchResult
src/main/resources/
    prompts/pdf-metadata.txt
    db/migration/V<n>__*.sql
    static/js/papers.js, static/js/search.js
src/test/java/.../slack/, papers/, search/
src/test/resources/fixtures/slack/, crossref/, arxiv/, html/, pdf/
deploy/smoke-check.sh, docs/ADMIN.md
```

Shared files: `pom.xml` + `requirements.txt` (add a library in the PR that needs it), `app.js` (only your `registerTab(...)` lines), `application.yml` (only the `slack:` and `papers:` sections).

---

## 2. Handoffs

See [SHARED.md](SHARED.md).

**Work that doesn't need any handoff:** `LinkExtractor`, the Slack app setup, and the Crossref/arXiv resolvers. A resolver only turns an HTTP response into a `ResolvedMetadata` record, so WireMock tests are enough. Test rung 3 with a fake `LlmClient` until H8 lands.

---

## 3. Tasks

### Phase 0: Foundations

- [x] Slack app "Pai project slack helper" in Socket Mode; `#scanner_testing` channel with the bot invited
- [x] Trigger decided: only when @mentioned (`app_mentions:read`, `app_mention` event); the mention message may contain other text
- [x] Share tokens with Sujal outside git → **H5**
- [x] Review the paper/slack tables in the V1 draft → **H3**
- [x] `LinkExtractor` + `LinkExtractorTest`:
  - Slack `<url|label>` and `<url>` forms
  - `arxiv.org/abs/…` and `/pdf/…`, with and without a version suffix
  - `doi.org/…`, DOIs inside Springer/ACM/Wiley URLs, IEEE `document/<id>` URLs
  - plain `.pdf` links
  - GitHub, YouTube, Google Docs and other non-paper links are ignored
- [x] URL normalisation (lowercase host, strip tracking params, strip arXiv version, `http`→`https`). The result is stored in `paper.normalized_url`
- [ ] Fixtures: messages from the test channel, recorded Crossref/arXiv responses

### Phase 1: Slack → database → Papers tab

- [x] `SlackAppConfig` (adds Bolt Jakarta Socket Mode + Tyrus): `App` + `SocketModeApp` started and stopped with Spring; tokens from env
- [x] `MessageEventHandler`: skip bot messages and edit/delete subtypes, ack right away, hand off to `IngestionService` via `@Async`
- [x] `IngestionService`:
  - insert `slack_message` with `ON CONFLICT (channel_id, message_ts) DO NOTHING`; if the row already existed, stop (it's a retry)
  - permalink via `chat.getPermalink`
  - for each link: upsert `paper` by `normalized_url` (PENDING), insert `paper_share`
- [x] `IngestionReplayTest`: the same event twice gives one message, one paper and one share
- [x] `PaperResolver` + `CrossrefResolver` + `ArxivResolver` (rung 1, adds WireMock for tests)
- [x] `PaperResolutionWorker`: `@Scheduled` every 30 s, claims PENDING rows with `FOR UPDATE SKIP LOCKED`, runs the ladder, sets `source`/`status`
- [x] `PaperController`: `GET /api/papers?status=`, `GET /api/papers/{id}`
- [x] `papers.js`: read-only list with title, authors, poster, date, Slack permalink, and a badge for MODEL/UNRESOLVED

### Phase 2: Full ladder + search

- [ ] If Slack events aren't arriving reliably, build `SlackPoller`: `@Scheduled` `conversations.history` with `oldest=<last ts>` per channel, a cursor table, feeding the same `IngestionService`. It's turned on by a config flag
- [x] `MetaTagResolver` (rung 2, adds Jsoup): real User-Agent, 10 s timeout, follow redirects; read `citation_title`, `citation_author`, `citation_doi`, `citation_publication_date`; if a DOI is found, send it back through rung 1
- [ ] `PdfLlmResolver` (rung 3, adds PDFBox): size cap (~20 MB), text from the first 2 pages, `LlmClient` + `prompts/pdf-metadata.txt`, `source = MODEL`
- [ ] References: store Crossref's `reference` array in `paper_reference` when present; otherwise the UI says "references not available"
- [x] Retry: an `attempts` column and a growing delay (`next_attempt_at`) between tries; after 3 failures the paper is marked UNRESOLVED. Nothing is ever deleted
- [ ] `ResolutionLadderTest`: known DOI → exact registry data; meta-tag page → `META_TAGS`; bare PDF → `MODEL`; dead link → `UNRESOLVED`
- [x] Search migration: generated `tsvector` columns + GIN indexes on `project`, `idea`, `paper` → **H10**
- [x] `SearchService`: `websearch_to_tsquery`, `ts_rank`, `UNION ALL` across the three tables, `type` filter, top 50
- [x] `SearchController`: `GET /api/search?q=&type=`
- [x] `search.js`: search bar in the header, results grouped by type, filter chips
- [x] Paper detail page + "Fix details" form → `PATCH /api/papers/{id}`, which sets `source = MANUAL`
- [x] `SearchServiceTest` on the seed data: one keyword across a project title, an idea and a paper returns all three

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

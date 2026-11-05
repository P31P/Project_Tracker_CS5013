# Priyal: Modules A, B, C (+ half of G)

The paper side of the platform: Slack ingestion (A), the paper archive and resolution ladder (B), and search + paper views (C). Shared phases, handoffs and schema: [SHARED.md](SHARED.md).

---

## 1. What you own

```
src/main/java/tracker/
    slack/      SlackAppConfig, MessageEventHandler, IngestionService, LinkExtractor, SlackPoller
    papers/     PaperController, PaperResolutionWorker, ResolutionLadder, resolver/*, dto/
    search/     SearchController, SearchService, SearchResult, SearchFilters
    people/     DirectoryController, DirectoryService
src/main/resources/
    prompts/pdf-metadata.txt
    db/migration/V<n>__*.sql
    static/js/papers.js, static/js/search.js, static/js/people.js
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
- [x] `PdfLlmResolver` (rung 3, adds PDFBox): size cap (~20 MB), text from the first 2 pages, `LlmClient` + `prompts/pdf-metadata.txt`, `source = MODEL`
- [x] References: store Crossref's `reference` array in `paper_reference` when present; otherwise the UI says "references not available"
- [x] Retry: an `attempts` column and a growing delay (`next_attempt_at`) between tries; after 3 failures the paper is marked UNRESOLVED. Nothing is ever deleted
- [x] `ResolutionLadderTest`: known DOI → exact registry data; meta-tag page → `META_TAGS`; bare PDF → `MODEL`; dead link → `UNRESOLVED`
- [x] Search migration: generated `tsvector` columns + GIN indexes on `project`, `idea`, `paper` → **H10**
- [x] `SearchService`: `websearch_to_tsquery`, `ts_rank`, `UNION ALL` across the three tables, `type` filter, top 50
- [x] `SearchController`: `GET /api/search?q=&type=`
- [x] `search.js`: search bar in the header, results grouped by type, filter chips
- [x] Paper detail page + "Fix details" form → `PATCH /api/papers/{id}`, which sets `source = MANUAL`
- [x] `SearchServiceTest` on the seed data: one keyword across a project title, an idea and a paper returns all three

### Phase 3: Stakeholder feedback (requests 3 and 4 in [SHARED.md](SHARED.md))

The tag filter, the People tab and the rebuilt search columns wait on Sujal's H11–H13. Paper tags (H14) don't, so start there.

**Paper tags (→ H14)**

- [ ] Migration: `paper.tags text[]` (default `'{}'`) with a GIN index
- [ ] `PaperDto` and `PaperEdit` carry `tags` (tidied like project tags); the paper Details page shows them, and **Fix details** edits them with Sujal's `tagPicker` (after H11; a plain comma-separated box until then)
- [ ] Tags edited on a paper don't change `source` or `status`: tagging is separate from fixing the details
- [ ] Optional, only when `llm.enabled`: after a paper resolves, suggest up to 3 built-in tags from its title (shown as "Suggested by AI", one click to keep). Skip it if time is short
- [ ] `PaperEditApiTest`: tags round-trip and are tidied

**Who shared a paper**

- [ ] Migration: `slack_message.poster_id` → `app_user`
- [ ] `IngestionService`: look up the poster's email (`users.info`, needs the `users:read.email` scope) and set `poster_id` when it matches an `app_user`; no match leaves it `null` and keeps the Slack name as now
- [ ] `IngestionReplayTest` (or a new test): a known email sets `poster_id`; an unknown one doesn't
- [ ] README: the new scope, and that the app must be reinstalled after adding it

**"Used in" on the paper page (after H15)**

- [ ] `GET /api/papers/{id}` adds the projects and ideas that list the paper as a reference; Details shows them as links

**Search tab with filters**

- [ ] Search migration (after H12 and H14): drop and re-add the `search` columns on `project` and `idea` with the new text fields (core question, motivation, approach, resources, challenges at weight B/C), and on `paper` with `tags` (weight C); recreate the GIN indexes; GIN indexes on `project.tags` and `idea.tags`. Sujal reviews it
- [ ] `SearchFilters` record: `type`, `tags` (all must match), `personId`, `from`, `to`
- [ ] `SearchService`: still one SQL query with a branch per table. Each branch adds:
  - date: `created_at` between `from` and `to` (both optional, whole days)
  - tags: `tags @> :tags`
  - person: projects they own or are a member of, ideas they wrote, papers they shared (`slack_message.poster_id`)

  A blank query with at least one filter lists everything that matches, newest first; a blank query with no filters still returns nothing
- [ ] `SearchResult` adds `date` and `tags` so the results can show them
- [ ] `SearchController`: `GET /api/search?q=&type=&tag=&tag=&person=&from=&to=`; a bad date or `from` after `to` gives 400 with a readable reason
- [ ] `search.js`: Search becomes a visible tab with its own search box at the top of the page and a filter bar under it:
  - type chips (as now)
  - tags with `tagPicker` (after H11)
  - a person picker (`GET /api/people`)
  - from and to dates
  - "Clear filters"

  The query and filters live in the hash (`#search/q=…&tag=…&person=…&from=…&to=…`), so a filtered search can be bookmarked and shared; an old `#search/<text>` link still works as a plain query
- [ ] Remove the header search box from `index.html` and the `initSearchBar` call in `app.js` (Sujal's files: tell him in the PR); a `/` shortcut opens the Search tab
- [ ] `SearchServiceTest`: each filter on its own, filters together, filters with no query, a person filter that finds a paper they shared, a date range that excludes old rows
- [ ] `SearchControllerTest`: repeated `tag` parameters; a bad date gives 400

**People tab**

- [ ] `tracker.people.DirectoryService` + `DirectoryController` (after H13). `GET /api/people` stays Sujal's slim list for member pickers:
  - `GET /api/people/directory`: everyone allowlisted with name, background, interests, and how many projects and ideas they have
  - `GET /api/people/{id}`: their `ProfileView` plus their projects (owned or member), ideas, and papers they shared
- [ ] `people.js`, registered as a People tab between Papers and Search:
  - a card per person (avatar, name, background, interests as tags, and an "Admin" or "Lab Manager" label from `role` so people know whom to ask for access)
  - filter by name and by interest (`tagPicker`); clicking an interest tag filters by it
  - `#people/<id>` opens a person's page: intro, background, interests, and their projects, ideas and papers
  - a person with an empty profile shows "No profile yet" instead of blank space
- [ ] Names of owners, members, authors and comment authors elsewhere link to `#people/<id>` (touches `projects.js` and `ideas.js`: ask Sujal, or leave it to him)
- [ ] `DirectoryApiTest`: only allowlisted people are listed (someone whose access was removed disappears); counts are right; an unknown id gives 404

### Phase 4: Deploy (your half of G)

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
| Feedback 3 | Every lab member appears in the People tab with their background and interests; a person's page lists their projects, ideas and shared papers; filtering by an interest works |
| Feedback 4 | Search is its own tab; date, tags, type and person filters work alone, together and without a query; a filtered search survives a page reload |

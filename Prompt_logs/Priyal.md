# Priyal - prompts for the Slack ingestion handoff

## 1. Understand the repository and handoff

Read the repository instructions, README, project plan, shared handoff notes, existing Slack link extractor, database entities and repositories, Spring configuration, and current tests. Summarize what the project does, what is already implemented, and the exact work Priyal owns for the current handoff. Do not edit files yet. Call out dependencies on earlier handoffs and any design decisions that need an answer.

## 2. Plan the implementation

Based on the repository and handoff documents, propose a small implementation plan for the Slack-to-database ingestion flow and the read-only papers API. List the files to add or change, how the Slack client will be used, how duplicate events will be handled, and how each part will be tested. Keep the work within Phase 1 and avoid starting the resolver ladder or search features. Ask only about decisions that cannot be inferred from the repo.

## 3. Configure Slack Socket Mode

Implement Spring-managed Slack Bolt and Socket Mode lifecycle wiring using the versions and conventions appropriate for this project. Read bot token, signing secret, app token, and enablement from configuration/environment variables. Keep Socket Mode disabled by default so local startup and CI do not require Slack credentials. Ensure the connection starts and stops with the Spring application.

## 4. Handle mentions promptly

Add a Slack `app_mention` handler. Ignore bot messages and edited/deleted message subtypes. Acknowledge valid events immediately to meet Slack's response deadline, then delegate ingestion to a background task. Use the existing Slack client from the event context and follow the project's Spring conventions.

## 5. Implement idempotent ingestion

Add an ingestion service that records each Slack message once using the existing unique channel/message timestamp key. If the message was already recorded, stop processing it. For a new message, try to fetch its Slack permalink; if that optional API call fails, continue ingestion. Reuse the existing `LinkExtractor`, find or create papers by normalized URL, and save a share for each extracted paper link. Keep message, paper, and share writes consistent with the existing entities and migrations.

## 6. Add the papers API

Add a small response DTO and signed-in read-only endpoints for `GET /api/papers` and `GET /api/papers/{id}`. Support an optional status filter on the list and return 404 for a missing paper. Use repository methods that fit the current Spring Data patterns and do not add unrelated API features.

## 7. Add focused tests

Add tests for ingestion replay, a message without paper links, and a message with multiple links. Stub the Slack permalink API rather than making network calls. Add MockMvc tests for listing, filtering, lookup, missing IDs, and unauthenticated access; use a Mockito repository stub where appropriate. Reuse the existing test base and test conventions. Do not require real Slack credentials in tests.

## 8. Update project documentation and review

Update the README with the new papers endpoints and Slack environment variables. Add a short human-readable work-log entry and record the prompts used for this handoff. Review the final diff for unrelated changes and summarize the files changed, tests added, and any verification that was or was not run. Do not claim tests passed unless they were actually executed.

# Priyal - prompts for the PR #10 review fixes

## 9. Fix a flaky test reported in review

PR #10 (branch `slack_integration`) has a review comment from Sujal: `SlackTablesTest.duplicatePaperUrlIsRejected` sometimes fails with `duplicate key value violates unique constraint "paper_normalized_url_key"` for `https://arxiv.org/abs/1706.03762`. The review says `IngestionReplayTest` is not `@Transactional`, so its papers stay in the shared embedded Postgres and collide with the same link in `SlackTablesTest` when it runs first.

Confirm that diagnosis by reading both tests, `PostgresTestBase` and `IngestionService`. Then fix it the way the other database tests already do (`@Transactional`), and remove any manual clean-up the annotation makes redundant. Keep the change inside the test files.

## 10. Check for the same problem elsewhere

Look at every other test that touches the database or the Slack tables and say whether it can leak rows into other tests. Fix only real leaks. Then run `mvn verify` once with `-Dsurefire.runOrder=reversealphabetical` so the order that failed is exercised, and report the number of tests that passed. Do not claim a pass unless the command was run.

## 11. Record the convention and the work

Add one line to the Testing conventions in `CLAUDE.md` saying database tests must be `@Transactional` because all tests share one database. Add a short work-log entry for today in `Work_logs/Priyal.md`. Add the prompts used here to `Prompt_logs/Priyal.md` as cleaned-up versions: each with the context, the goal, the constraints and how to check the result. Do not stage, commit or push. Finish by listing the git commands I should run to update PR #10.

# Priyal - prompts for the paper resolvers (rung 1)

## 12. Pick the next handoff and plan the branch

PR #10 (Slack ingestion) is still open. Read `plans/PRIYAL.md` and `plans/SHARED.md` and tell me the next unchecked Phase 1 item for me and which of it needs no handoff from Sujal. Propose a small branch for just that item: the files to add, any dependency it needs and how it will be tested. Do not start the worker, the database migration or the UI. Tell me how to branch while PR #10 is unmerged.

## 13. Build the Crossref and arXiv resolvers

Add `PaperResolver`, `CrossrefResolver` and `ArxivResolver` in `tracker.papers.resolver`. A resolver takes a `normalized_url` (see `LinkExtractor` in `CLAUDE.md`) and returns title, authors, publication date and DOI, or empty if the source doesn't know the paper. Throw on network or server errors so the worker can retry later. Use Spring's `RestClient` with a 10 s timeout and a User-Agent, and the JDK XML parser with DOCTYPE disabled for arXiv's Atom feed, so the only new dependency is WireMock (test scope, added to `pom.xml` and `requirements.txt` together). Keep the code simple enough that both of us can explain every line.

## 14. Test with recorded responses

Record real responses from `api.crossref.org` and `export.arxiv.org` into `src/test/resources/fixtures/`, including a known item, an unknown ID and (for arXiv) a malformed ID. Write WireMock tests with no Spring context for: a known DOI and arXiv ID, not found giving empty, a year-only date, an organisation author, a record with no title and a 503 being thrown. Run `mvn verify` and report the real counts.

## 15. Record the work

Update the README status line, tick the resolver item in `plans/PRIYAL.md`, and add a work-log entry and these prompts to the logs. Do not stage, commit or push; list the git commands to create the branch and open the PR.

## 16. Plan the worker branch

The resolvers PR is open and stacked on #10. Start a new branch from it for the worker only: a scheduled job that takes PENDING papers, runs the resolvers and saves the result. Before writing code, list the schema changes, how a paper is claimed so two instances can't take the same row, and what happens when a resolver throws, so a permanently failing paper cannot starve newer ones. Keep the Phase 2 rungs (meta tags, PDF, LLM) out of it.

## 17. Build the worker

Add migration `V4` for `paper.title`, `authors`, `published_on`, `doi`, `source` and `attempts`, update the `Paper` entity and `PaperDto`, and add `PaperResolutionService` (one paper per transaction, claimed with `FOR UPDATE SKIP LOCKED`) and `PaperResolutionWorker` (`@Scheduled`, up to 10 papers per run). A resolver returning empty or no resolver supporting the URL gives UNRESOLVED. An exception keeps the paper PENDING and counts an attempt, becoming UNRESOLVED on the third. The worker must be off in tests so they never reach Crossref or arXiv; add a `papers.resolution-enabled` setting for that.

## 18. Test and verify

Test the service against the embedded Postgres with a mocked resolver: resolved, not found, unsupported link, three failures, a failing paper not blocking a newer one, and nothing pending. Read back from the database rather than the cached entity. Add a test that the worker calls the service on a schedule, and one for the new API fields. Then run a throwaway check against the real Crossref and arXiv APIs, delete it, and report the real test counts from `mvn verify`.

## 19. Plan the next branch without handoffs

Sujal has not opened any PRs yet, so anything that needs his code (`LlmClient`, the dashboard shell, `idea` table) is blocked. From `plans/PRIYAL.md`, pick the next item that needs nothing from him. Branch from the worker branch and list the files, the one dependency it adds and the tests, before writing code. Keep PDF/LLM work out of it.

## 20. Build the meta-tag resolver (rung 2)

Add `MetaTagResolver` (Jsoup, 10 s timeout, follows redirects, browser-style User-Agent). Read `citation_title`, `citation_author`, `citation_doi` and the publication date. If the page names a DOI, pass it through `CrossrefResolver` and prefer that result. Return empty for a bare PDF, a 401/403/404/410 and a page with no `citation_title`; throw on timeouts and other server errors so the worker retries. Add `source` to `ResolvedMetadata` so a paper records whether it came from the registry or from page tags, and use `@Order` so the ladder is Crossref, arXiv, meta tags. Keep it readable for both of us.

## 21. Test with recorded pages, then check live

Record the citation meta tags of a real Springer and a real Nature page into `src/test/resources/fixtures/html/`. Test with WireMock: tags read correctly, the Crossref upgrade, no `citation_title`, a bare PDF, a 403 and a 503. Then run a throwaway check against real links (an arXiv link, an arXiv-registered DOI, a Nature page, an IEEE page, a bare PDF, a fake DOI), delete it, and tell me what each ended up as and any limits you saw. Report real test counts. Do not stage, commit or push.

## 22. Run the real Slack flow once

The Slack code and the paper worker are merged into my branch but have only been tested with stubs. I want to see it work for real in `#scanner_testing`. Check what is missing on this machine (database, `.env`) without printing any secrets, tell me exactly what I have to set up and where each value comes from, then start the app and watch the logs while I post a mention with an arXiv link and a DOI. Afterwards read the database and tell me what was saved and what is still untested. Do not change code or commit anything.

## 23. Plan the Papers tab without the dashboard shell

Sujal's shell (`index.html`, `app.js` with `registerTab`) isn't on `main`, so build the tab so it doesn't depend on it: `static/js/papers.js` as an ES module exporting `renderPapers(container)`, loading its own stylesheet, so hooking it up later is one line in `app.js`. Plain JS, no libraries. List what each paper row shows (title or URL, authors, year, who shared it and when, a Slack link, badges for PENDING, UNRESOLVED and `source=MODEL`) and say which data the API doesn't return yet. Branch from the meta-tag branch and don't touch Sujal's files.

## 24. Add the Slack fields to the API and the tab

Return, for the first share of each paper, the poster's name, the time and the Slack permalink in `PaperDto`, fetched in one query for the whole list (no query per paper). Look up the poster's name when a message is ingested, tolerating failure like the permalink does, and tell me which Slack scope that needs. Write `papers.js` using only DOM calls and `textContent` (titles come from outside websites), ignore any link that isn't http(s), and re-check every 10 s while a paper is still PENDING. Test the API against the real Postgres, including no papers and a paper with no share.

## 25. Check how it looks

I can't run the app without Google sign-in, so serve the static files with a throwaway local server and a stub `/api/papers` (keep it outside the repo), and screenshot the page in headless Edge: a normal list, an empty list, a narrow window, and a title containing HTML to prove it is shown as text. Fix anything that overflows. Report real test counts and say what is not covered (there are no JS tests because the project has no Node).

# Project_Tracker_CS5013
Course project for CS5013 : Programming with AI

A tracker for Raman Lab (DSAI, IIT Madras): it tracks projects and ideas, archives paper links from Slack automatically, and searches across all of them.

## Status
Google sign-in with an allowlist; database tables for users, projects, Slack messages and papers. Slack mentions can now be ingested into the paper archive, and the papers API is available. The dashboard has four tabs: **Home** (your profile, lab numbers, your projects and ideas, popular ideas, and recent activity with a **For you** view of new ideas and papers that match your interests), **Projects** (list or drag-and-drop board; start with just a title, then add members, tags, links and timeline updates), **Ideas** (quick capture with tags, upvotes, a discussion under each idea, turn an idea into a project) and **Papers**. Signed-out visitors land on a sign-in page; a System / Light / Dark theme switch sits beside the account menu, which has Sign out. The interface uses the Geist typeface, bundled in `static/fonts/` (SIL Open Font License). Project descriptions, timeline updates, idea descriptions and comments support Markdown (bold, italic, code, lists, headings, quotes, links). A background worker fills in each new paper's title, authors and date from Crossref (DOI links), arXiv, and the citation tags on publisher pages. The search box in the header finds projects, ideas and papers (needs `GET /api/search`). With auto-fill on, a **Suggest** button in the New project dialog and the idea box turns a rough note into a title, description and tags for you to check before saving.

## Run locally
Needs Java 21, Maven and PostgreSQL with a `tracker` database (see [SCAFFOLDING.md](SCAFFOLDING.md), Step 0).
```bash
mvn verify
mvn spring-boot:run -Dspring-boot.run.profiles=local
```
Then open http://localhost:8080. For demo data on a local database (16 projects, 12 ideas, with members, updates, votes and comments), run `psql -U tracker -d tracker -1 -v ON_ERROR_STOP=1 -f src/test/resources/seed-mock-data.sql` once.

## API
Signed-out API calls get `401`; pages redirect to the sign-in page (`/login.html`).
- `GET /api/me`: email and name of the signed-in user
- `GET /api/papers`: list all archived papers with title, authors, date and who first shared them in Slack (optional `?status=PENDING|RESOLVED|UNRESOLVED`)
- `GET /api/papers/{id}`: single paper
- `PATCH /api/papers/{id}`: correct a paper's details (`title` required; `authors`, `publishedOn` and `doi` replace what is stored). Marks it `RESOLVED` with source `MANUAL`. In the Papers tab, open a paper's **Details** and use **Fix details**
- `PUT /api/papers/{id}/tags` (`tags`): replace a paper's tags, tidied like project tags; doesn't change its status or source
- `PUT`/`DELETE /api/papers/{id}/vote`: upvote a paper or take the vote back; `PUT`/`DELETE /api/papers/{id}/read`: mark it read or unread for yourself only. Paper responses carry `tags`, `voteCount`, `votedByMe` and `readByMe`, and the Papers tab filters by Unread/Read and sorts by votes
- `GET /api/papers/{id}/references`: the paper's reference list in order (`position`, `citation`, `doi`), shown under **References** on its Details page. Only papers found in Crossref have one; papers resolved before this was added get theirs only if they are looked up again
- `GET /api/search?q=&type=`: projects, ideas and papers matching the text (web-search syntax: `"exact phrase"`, `or`, `-exclude`), best first, at most 50; `type` is optional (`PROJECT`, `IDEA` or `PAPER`)
- `GET /api/overview`: counts, the signed-in user's projects and ideas (`myIdeas`), the latest activity, and `forYou`: ideas by others and papers from the last 30 days whose tags (or, for a paper, title) match one of the user's interests, leaving out papers they marked as read
- `GET /api/profile`, `PATCH /api/profile` (`bio` in Markdown, `background`, `interests`; all three replace what is stored): the signed-in person's own profile
- `GET /api/people`: everyone who can sign in (to add as project members)
- `GET /api/projects`, `POST /api/projects` (`title` required; `description`, `status`, `tags`, `memberIds` optional), `GET`/`PATCH /api/projects/{id}`, `POST /api/projects/{id}/updates`, `POST /api/projects/{id}/links` (`url` must be http(s); `label` optional), `DELETE /api/projects/{id}/links/{linkId}`
- `GET /api/autofill`: whether auto-fill is on; `POST /api/autofill` (`note` required): a suggested `title`, `description` and `tags`, never saved
- `GET /api/ideas`, `POST /api/ideas` (`title` required; `description`, `tags` optional), `PUT`/`DELETE /api/ideas/{id}/vote`, `GET`/`POST /api/ideas/{id}/comments` (`body` required), `POST /api/ideas/{id}/promote` (starts a project in Planning with the idea's tags)

Requests that change data need the `X-XSRF-TOKEN` header (the browser gets it from the `XSRF-TOKEN` cookie; `static/js/api.js` sends it). Errors come back as `{"error": "..."}`.

## Configuration
Settings live in `src/main/resources/application.yml`. Override them with env vars or a local `.env`: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `PORT`, `PAPER_RESOLUTION_ENABLED` (default `true`; set `false` to stop the worker calling Crossref and arXiv).

Auto-fill (optional) uses a language model through an OpenAI-style API, by default a local [Ollama](https://ollama.com), so notes never leave the machine: install Ollama, run `ollama pull llama3.2:3b`, and set `LLM_ENABLED=true`. `LLM_BASE_URL` (default `http://localhost:11434/v1`), `LLM_MODEL` (default `llama3.2:3b`), `LLM_API_KEY` (blank for Ollama) and `LLM_TIMEOUT` (default `60s`) point it elsewhere. Off by default; every form works without it. When it is on, the paper worker also uses it as a last resort for bare PDF links: it reads the first two pages and fills in the title, authors and year, marked "Guessed by AI, please check" until someone fixes them by hand.

Sign-in uses Google: set `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` from a Google Cloud OAuth client (Web application, redirect URI `http://localhost:8080/login/oauth2/code/google`).

Slack ingestion (Socket Mode): set `SLACK_BOT_TOKEN`, `SLACK_SIGNING_SECRET`, `SLACK_APP_TOKEN` and `SLACK_SOCKET_MODE_ENABLED=true`. The app starts without these; omit them in CI. If Slack can't connect (bad token, outage), the error is logged and the rest of the app keeps running. To try it, start the app, mention the bot with a paper link in the channel, and check `GET /api/papers` (or the `paper` table): within about 30 s the paper has its title and authors. The poster's name needs the `users:read` bot scope (reinstall the app after adding it); without it papers are shown without a name. The app needs `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` to start; any placeholder value works if you only test Slack and read the database.

## Docs
- [PLAN.md](PLAN.md): where the plans are
- [plans/](plans/): shared phases/handoffs/schema and per-person plans
- [SCAFFOLDING.md](SCAFFOLDING.md): how to set up the project skeleton
- [requirements.txt](requirements.txt): what to install (grows stage by stage)
- [CLAUDE.md](CLAUDE.md): rules for AI coding assistants
- [Work_logs/](Work_logs/): what each of us has done

## License
[MIT](LICENSE) © 2026 Priyal Patel and Sujal Jayant Kumar

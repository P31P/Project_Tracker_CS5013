# Sujal - prompts for planning and the repository set-up

## 1. Plan the whole project from the proposal

We are building a tracker for Raman Lab: projects, ideas and the papers people share in Slack. Read `Proposal_Doc.pdf` and `Design_Doc.pdf` and write a plan for the whole workflow: phases, what each phase delivers, the modules and the database tables. The backend has to be Java (the course requires it), so tell me whether we really need Docker or docker-compose for a two-person project that runs locally first. Include the repository tree the plan will produce. Do not write code yet. Do not invent requirements that aren't in the two documents.

## 2. Split the plan so we can work in parallel

Split the plan into one shared file and one file per person (`plans/SHARED.md`, `plans/PRIYAL.md`, `plans/SUJAL.md`) so Priyal and I can work independently. Mark every point where one of us needs something from the other as a numbered handoff, and keep the schema draft in the shared file. Leave out logistics such as meeting dates or emails; only say what has to be built and in what order. Keep `PLAN.md` as a short pointer to the plans folder. Don't give either of us a task that silently depends on the other's unfinished work.

## 3. Write rules for AI assistants

Write `CLAUDE.md` (and an `AGENTS.md` with the same rules, since Priyal may use a different assistant) with: never stage, commit or push; minimal comments; update the README with one line per user-visible change; build sequentially and never add packages or config for features that aren't being built yet; add dependencies one at a time to `pom.xml` and `requirements.txt` together; stay inside your own module. Add the stack, the commands and who owns which module. Keep it to short rules; don't explain each one at length.

## 4. Write a scaffolding guide and the first files

Write `SCAFFOLDING.md`: a step-by-step guide to the project skeleton with the repository structure as a table. Fill `.gitignore` with sensible defaults for Java, Maven and editors, and start `requirements.txt` with only what the skeleton needs; we will add to it as features land. Add a GitHub Actions `ci.yml` that runs `mvn verify`, and tell me whether the free minutes are enough for us. Do not list dependencies for features we haven't started.

# Sujal - prompts for the project skeleton (H1)

## 5. Build the skeleton

Following `SCAFFOLDING.md`, create the Spring Boot 4.1 project on Java 21 with Maven: `pom.xml` with only the scaffold dependencies, `application.yml` reading settings from env vars and a git-ignored `.env`, Flyway, a placeholder page and the CI workflow. Tests must run against a real embedded Postgres (Zonky), never H2, and without Docker. Do not add anything for the LLM or later phases. Run `mvn verify` and start the app to show it works. Don't add Docker or a database container.

## 6. Simplify the skeleton

Rename the base package from `in.ac.iitm.ramanlab.tracker` to `tracker`; it is easier to read and we are not publishing a library. Remove the Maven wrapper (`mvnw`, `mvnw.cmd`) and list Maven in `requirements.txt` instead, and drop `.env.example`, which two developers don't need. Explain why tests live under `src/test` and what `src/main/resources` is for. Add an MIT licence under both our names and update the work log. Do not change any behaviour while renaming.

# Sujal - prompts for the database schema (H4)

## 7. First migration and entities

Write `V1__init.sql` with `app_user` (unique email, an allowlist flag) and `project`, where only the title is required and a blank title is rejected by the database. Add matching JPA entities and repositories in `tracker.data`. Name migrations with plain numbers (`V1`, `V2`, …), not dates. Add `ConstraintsTest` against the embedded Postgres: a project saves with only a title, a blank title fails, a duplicate email fails. Explain how the entities relate to the SQL, since Flyway owns the schema. Don't add columns that no feature uses yet.

## 8. Slack tables for Priyal's ingestion

Write `V2__create_slack_tables.sql` with `slack_message`, `paper` and `paper_share`, matching what Priyal's `LinkExtractor` produces (one `normalized_url` per paper). A Slack message must be stored once (unique channel and timestamp), a paper once per normalized URL, and new papers start as `PENDING`. Store the poster's Slack user name as well as the id. Add entities, empty repositories and `SlackTablesTest` for the duplicate rules and the default status. Give me commit messages and a PR description, stacked on the previous PR. Do not add repository methods until a feature needs them.

# Sujal - prompts for Google sign-in (H6)

## 9. Sign-in with an allowlist

Add Google sign-in with Spring Security's OAuth2 client. Every page requires sign-in, and only `app_user` rows with `allowlisted = true` get in; add both of us (`da24b026@smail.iitm.ac.in`, `cs24b047@smail.iitm.ac.in`) with a migration. Read the client id and secret from `.env`, and make sure CI still passes without them. Add `GET /api/me` returning the signed-in user's email and name. Go step by step and tell me what to set up in Google Cloud. Never put the client secret in the repository or print it.

## 10. Test the sign-in rules

Add tests for: an allowlisted user gets in, an unknown or non-allowlisted user is refused, a signed-out visitor is redirected, and `/api/me` returns the right user. Use `oidcLogin()` from spring-security-test rather than a real Google account. Report the real test count, then update the README, the plan and the work log. Do not call real Google from tests, and don't claim tests passed unless you ran them.

# Sujal - prompts for two PR reviews

## 11. Review PR #10 (Slack ingestion) by running it

Priyal opened PR #10. Pull the branch and review it by running it, not just reading it: start the app in the situations the lab will really hit, including Slack being unreachable and an expired or mistyped token, and look for missing edge cases in the tests. For each real problem give the steps to reproduce it, a small test that fails now and passes with the fix, and the smallest fix. Don't change her branch. Draft the review comment for me to post. Don't report style nitpicks or anything you couldn't reproduce.

## 12. Review PR #13 (paper resolution worker)

Review the worker in PR #13 with a focus on what happens when Crossref or arXiv fail. Follow one failing paper through a whole scheduled run (`resolveNext()` up to 10 times) and check whether a short outage could use up all its retries. If so, write a test that shows it and a minimal diff that spaces the retries out. Draft the review comment for me to post. Do not suggest catching every exception just to make the problem disappear.

# Sujal - prompts for the dashboard shell and sign-in page (H7)

## 13. Build the shell

Write the dashboard shell in plain JavaScript (no Node, no build step): `index.html` with a header, tab bar and content area; `app.css` with light and dark themes and a phone layout; `api.js` with a fetch wrapper that sends the user to sign-in on a `401`; and `app.js` with `registerTab(id, label, render, description)` and the tab taken from the URL hash. Register Priyal's existing Papers tab with one line. Build pages with DOM calls and `textContent`, never `innerHTML`, because titles come from outside sources. Do not edit Priyal's `papers.js`.

## 14. Let the page react to being signed out

Make `/api/**` answer `401` instead of redirecting to Google, so the page's JavaScript can react, while normal pages still redirect. Make sure a refused user can't end up in a redirect loop, and add tests for both. Check the behaviour on the running app as well, not only in MockMvc. Don't write a custom error handler where Spring already offers one.

## 15. Add a sign-in page

Signed-out visitors should land on a starting page with a "Sign in with Google" button instead of going straight to Google. Add `login.html` (open without sign-in, as is `/css/**`), show a clear message when an account isn't allowlisted, and add sign-out to the account menu that returns to this page. Keep it in the shell's style. Do not put any client id or secret in the page.

## 16. Design pass for the mid demo

Make the shell look close to a finished product without making it heavy: a logo, a sticky header, an account menu, page headers with an action area, toasts, loading skeletons and empty states, and a two-column sign-in page. Follow common design principles (spacing, contrast, focus states, reduced motion) and keep the CSS readable. Check light, dark and phone layouts with headless Chrome screenshots. Do not add a CSS framework, icon font or web font.

## 17. CSRF and sign-out

Turn CSRF protection on for the API in the way a JavaScript page can use it (`csrf().spa()`), make sure the token cookie is always written, and have `api.js` send it as `X-XSRF-TOKEN` on every POST, PATCH and DELETE. Sign-out must be a POST with the token. Add tests that a change without the token is refused, and add the convention to `CLAUDE.md`. Don't switch CSRF off for the API to make tests easier.

# Sujal - prompts for projects and ideas (module E)

## 18. Projects API

Add projects with a timeline: migration `V5` (status `PLANNING`/`ACTIVE`/`ON_HOLD`/`DONE` with a default, `updated_at`, and a `project_update` table), entities, `ProjectService` and `ProjectController`. Only the title is required. A status change is logged on the timeline automatically, and the list is ordered by recent activity. Errors come back as `{"error": "..."}` with a readable message. Find the signed-in user's row through one helper. Test it against the embedded Postgres, including a request without the CSRF token. Do not add fields the plan doesn't ask for.

## 19. Ideas API

Add ideas (`V6`): capture with only a title, list newest first, and "promote" an idea into a Planning project linked back to it, allowed only once (`409` on the second try, `404` for an unknown idea). Reuse the project service for promotion rather than duplicating it. Test the happy path and each error. Don't delete the idea when it is promoted.

## 20. Projects and Ideas tabs

Build `projects.js` and `ideas.js` on the shell: a project list with status filters, a "New project" dialog with only the title required and the rest under "More details", a project page with a status picker, edit dialog, update composer and timeline; an ideas page with quick capture, filters and "Turn into a project". Since Google sign-in blocks automated testing, drive both tabs end to end in headless Chrome against a throwaway stand-in API kept outside the repo, and fix whatever breaks. Do not commit the stand-in API or the screenshots.

## 21. Finish the UI side before the mid demo

The pages look too simple. After the mid demo we only want to do auto-fill and hosting, so finish the UI now. Ask me which fields and features to add, then build what I pick: project members, links and tags (`V7`, new migrations only because `V5`/`V6` are already applied locally), idea tags and upvotes (`V8`, one vote per person), a Home tab with counts, my projects, popular ideas and a recent-activity feed, and a board view where dragging a card changes its status. Keep the current clean style with line icons, coloured avatars and tags and subtle animations. Only `http(s)` links may be stored or rendered. Add API tests, drive every feature in headless Chrome, check light, dark and phone layouts, and report the real test count. Do not edit migrations that are already applied, and don't add any new dependency.

## 22. Find out why a paper is stuck on "Looking up details…"

Two arXiv papers have been stuck on "Looking up details…" for a while. Read the database without printing secrets, check whether anything I changed could affect the worker, and reproduce the exact request the app makes. Tell me the cause and when they will resolve. If the fix is in Priyal's module, describe it rather than changing her code. Don't stop or restart my running app.

# Sujal - prompts for documentation and understanding the code

## 23. Javadoc for every Java source

Add Javadoc to every public class, record, constructor and method under `src/main/java`, plus the package-private helpers other classes call, following common industry practice: a one-sentence summary, then `@param`, `@return` and `@throws` where they apply, record components as `@param`, and controllers naming their endpoint. Overrides inherit the interface's doc. Keep each doc short and accurate to what the code really does; read a method before describing it. Check with `javadoc -Xdoclint:all` and confirm the diff contains only comment lines. Do not change any code, and do not document the tests. Priyal's packages are included, so tell me which of her files were touched.

## 24. Explain the architecture so I can defend every line

Write me a guide to the whole codebase: the layers a request passes through, the sign-in, Slack and paper-resolution flows step by step, the database migrations, and every Java file with each method explained in plain words. Separately, explain how Google OAuth works on Google's side and in our code, the difference between SQL migrations, Flyway and JPA, what IAM users are, and why we need both Google sign-in and our own allowlist. Check claims against the code rather than guessing. Don't change any code while doing this.

# Sujal - prompts for the review feedback

## 25. Let people pick a light or dark theme

The app only follows the computer's light or dark setting. Add a System / Light / Dark choice that is remembered in the browser and also applies to the sign-in page. The saved theme must be applied before the page is drawn so it never flashes the wrong colours, and the sign-in page cannot load scripts from `/js/` before sign-in. Keep the colours as CSS variables and note in `CLAUDE.md` anything that must be changed in two places. Check it in headless Chrome with the computer in dark mode. Don't add a library for this.

## 26. A discussion under each idea

Add comments to ideas so people can discuss them: a table, `GET` and `POST /api/ideas/{id}/comments`, a comment count on each idea, and a "Discuss" button that opens the thread under the card. Comments read oldest first; a blank comment gets a readable error. Take the migration number from the repo's convention and tell me if another open branch uses the same one. Test the order, the count, a blank comment, an unknown idea and a missing CSRF token. Show comment text with DOM calls, never `innerHTML`, and don't add editing or deleting comments yet.

## 27. Fix the app refusing to start after switching branches

`mvn spring-boot:run` fails with a Flyway validation error after I switched branches. Read the migration history in my local database without changing anything, explain the cause, and tell me why `flyway repair` would or wouldn't be right here. If local data has to be removed, first check that nothing depends on it, then do it in one transaction. Start the app on another port with Slack and the paper worker off to prove it works. Don't touch other branches or the stash, and don't stop my own app.

## 28. Markdown in descriptions, updates and comments

Support Markdown where people write longer text: project descriptions, timeline updates, idea descriptions and comments. Cover bold, italic, inline code, code blocks, lists, headings, quotes and links. Previews that are cut to two lines should show plain text without Markdown symbols. Pasted HTML must stay visible text and only `http(s)` addresses may become links, so test `<script>`, `<img onerror>`, `javascript:` and `data:` links, names like `file_name` and `2 * 3 * 4`. Do not use a library that produces an HTML string, and do not insert HTML with `innerHTML`.

## 29. Design pass in the style of Emil Kowalski, impeccable and taste

Use Emil Kowalski's approach to motion and the "impeccable" and "taste" guidance against generic AI-looking design for the whole website. If those skills aren't installed, say so and apply their principles. Replace the system font with a real typeface stored in the app, use tinted neutral colours, and remove clichés such as gradient logos, background glows, coloured side stripes, icon tiles above numbers and rainbow pills. Motion should be fast and ease out, with a small press on controls, dialogs and menus that grow in rather than pop, hover effects only on devices that hover, and nothing that replays on every redraw. Respect reduced motion. Check light, dark and phone layouts with screenshots and rerun the earlier browser checks. Don't change how anything works, and don't load fonts or scripts from another website at runtime.

## 30. Move the theme switch beside the account menu

Move the System / Light / Dark switch out of the account dropdown and put it in the header beside the avatar, as a compact icon-only control. Each icon still needs a tooltip and a label for screen readers. Check desktop in both themes and a phone-width header, and update every comment and document that says the switch is in the account menu. Don't change how the theme is saved or applied.


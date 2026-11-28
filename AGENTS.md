# Project
Clojure project (GALT)

Before non-trivial Clojure work, read `PROJECT_SUMMARY.md` at the repo root and treat it as the map of modules, entry points, and tools. If it is missing or stale, say so instead of guessing structure.

Also follow `LLM_CODE_STYLE.md` at the repo root when present; it matches the global Clojure style rules.

## From a clean checkout
Needs only a JDK, the Clojure CLI, git and docker or podman on the machine. No `.env` and no other setup.

- `./scripts/dev.sh up` starts a Postgres container and runs the migrations. The first run imports the bundled geodata sample; `./scripts/dev.sh geodata` switches to the full dataset, `./scripts/dev.sh geoip` downloads the GeoIP database.
- `./scripts/dev.sh run` serves http://localhost:8081. With `GALT_ENV` unset it runs the `:local` system (`config/default-config.edn`, files stored on disk).
- `clojure -M:nrepl` starts a REPL, `clojure -M:test` runs every test.
- `./scripts/dev.sh down` stops Postgres and keeps the data, `./scripts/dev.sh reset` drops the data and starts again.

Agents work this way by default. Start clj-contain only when asked: it is the manual containerized workflow, which sets `GALT_ENV=dev` and stores files in the shared dev R2 bucket.

## Clojure workflow
- Use clojure-mcp tools for all Clojure work: `clojure_eval`, `clojure_edit` / `clojure_edit_replace_sexp`, `scratch_pad`. Prefer structural editing over raw file edits.
- REPL-driven: prototype in `clojure_eval`, test edge cases, then save code.
- Reload namespaces with `(user/go!)` (need to evaluate the namespace `dev.user` in the REPL before the first time); switch into the namespace you're working on.
- Dev loop: `(galt.main/start-system!)` to start (`:local` in a plain checkout, `:dev` under clj-contain), `(go!)` to hot-reload
  (reloads changed namespaces, restarts HTTP server, after calling it the running system reflects the code saved in the project directory, including dependencies (local or remote))

## Addresses

How Galt is reached depends on where OpenCode runs:

- **Inside the `clj-contain` container**: the host publishes the app on port 13003, and the browser runs on the host, so the chrome-devtools MCP must use http://127.0.0.1:13003/. Command-line tools inside the container talk to the app directly at http://localhost:8081/.
- **Natively (plain checkout, agent sandbox)**: everything runs on one machine, so both the chrome-devtools MCP and command-line tools use http://localhost:8081/.

`scripts/chrome-devtools-mcp.sh` picks the transport: the SSH bridge when `/run/cdp-key` and `host.containers.internal` exist (the container case), otherwise a local `chrome-devtools-mcp` against the installed Chrome (`CHROME_BIN`, `CHROME_HEADED=1` to run headed).

1. When accessing Galt web page through chrome-devtools MCP use the base URL for the environment above
2. When accessing Galt web page through curl or other command line tools use http://localhost:8081/ as base URL

## Login and payment workflows for programmatic testing

1. If logged in user session is needed, log in fom http://localhost:13003/members/login (container) or http://localhost:8081/members/login (native) using `bb scripts/lnurl-login.bb <script-username> <LNURL-string>`
  - the LNURL string is in the text input on the /members/login page if the user is not logged in
  - the available usernames are `alice`, `bob` and `charlie` (listed in `scripts/lnurl-login.edn`)

## Tailwind (new design)

The Learn pages use Tailwind CSS v4 with the layout in `src/galt/core/views/new_layout.clj`. Reference markup and tokens are in `docs/design/learn/`.

- Tailwind classes go in `:class` as a vector of strings: `[:div {:class ["flex" "gap-2"]}]`.
- Write complete static class names only. Never build class names at runtime; the Tailwind scanner must see them.
- Move long or repeated class lists into named constants or small component functions.
- Prefer `@theme` tokens over arbitrary values.
- Keep the class order: layout, spacing, typography, colors, effects, states, responsive.
- Use semantic, accessible markup. Avoid `@apply`.
- Run `npm run css` after adding classes. The generated CSS is not committed.

### Domain docs

Single-context: one `GLOSSARY.md` and `docs/adr/` at the repo root. See `docs/agents/domain.md`.

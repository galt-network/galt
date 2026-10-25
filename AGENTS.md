# Project
Clojure project (GALT)

Before non-trivial Clojure work, read `PROJECT_SUMMARY.md` at the repo root and treat it as the map of modules, entry points, and tools. If it is missing or stale, say so instead of guessing structure.

Also follow `LLM_CODE_STYLE.md` at the repo root when present; it matches the global Clojure style rules.

## Clojure workflow
- Use clojure-mcp tools for all Clojure work: `clojure_eval`, `clojure_edit` / `clojure_edit_replace_sexp`, `scratch_pad`. Prefer structural editing over raw file edits.
- REPL-driven: prototype in `clojure_eval`, test edge cases, then save code.
- Reload namespaces with `(user/go!)` (need to evaluate the namespace `dev.user` in the REPL before the first time); switch into the namespace you're working on.
- Dev loop: `(galt.main/start-system! :dev)` to start, `(go!)` to hot-reload
  (reloads changed namespaces, restarts HTTP server, after calling it the running system reflects the code saved in the project directory, including dependencies (local or remote))

## Project Summary

When asked to create or update `PROJECT_SUMMARY.md`, load the `update-project-summary` skill and follow it.

## Addresses

When the project is developed in a container, the container exposes ports on localhost on the host machine on port 13003.
This is relevant for the chrome-devtools MCP server as it is controlled by the OpenCode instance inside the container but accesses chrome on the host machine. Therefore:
1. When accessing Galt web page through chrome-devtools MCP use http://127.0.0.1:13003/ as base URL
2. When accessing Galt web page through curl or other command line tools use http://localhost:8081/ as base URL

## Login and payment workflows for programmatic testing

1. If logged in user session is needed, log in fom http://localhost:13003/members/login using `bb scripts/lnurl-login.bb <script-username> <LNURL-string>`
  - the LNURL string is in the text input on the /members/login page if the user is not logged in
  - the available usernames are `alice`, `bob` and `charlie` (listed in `scripts/lnurl-login.edn`)

## Progress tracking (PROGRESS.md)

Before non-trivial work:
1. Read `PROGRESS.md` (create it from the template if missing).
2. Read `PROJECT_SUMMARY.md` for architecture (already required).

Before ending a session or handing off:
1. Update `PROGRESS.md`:
   - Refresh Current Verified State
   - Move finished items to Completed (include evidence: commit, test, or REPL result)
   - Update In Progress / Next Steps / Blocker
   - Append a short Session Log entry
2. Do not mark anything done without verification.

Keep the file concise. Prefer editing current-status sections over pure append-only growth.

## Agent skills

### Issue tracker

Issues and specs live as markdown files in `.scratch/<feature-slug>/`. See `docs/agents/issue-tracker.md`.

### Triage labels

Five canonical labels: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `GLOSSARY.md` and `docs/adr/` at the repo root. See `docs/agents/domain.md`.

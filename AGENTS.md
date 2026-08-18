Clojure project (GALT). Project context is preloaded: see PROJECT_SUMMARY.md.

## Clojure workflow
- Use clojure-mcp tools for all Clojure work: `clojure_eval`, `clojure_edit` / `clojure_edit_replace_sexp`, `scratch_pad`. Prefer structural editing over raw file edits.
- REPL-driven: prototype in `clojure_eval`, test edge cases, then save code.
- Reload namespaces with `(user/go!)` (need to evaluate the namespace `dev.user` in the REPL before the first time); switch into the namespace you're working on.
- Dev loop: `(galt.main/start-system! :dev)` to start, `(go!)` to hot-reload
  (reloads changed namespaces, restarts HTTP server, after calling it the running system reflects the code saved in the project directory, including dependencies (local or remote))

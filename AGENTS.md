Clojure project (GALT). Project context is preloaded: see PROJECT_SUMMARY.md.

## Clojure workflow
- Use clojure-mcp tools for all Clojure work: `clojure_eval`, `clojure_edit` / `clojure_edit_replace_sexp`, `scratch_pad`. Prefer structural editing over raw file edits.
- REPL-driven: prototype in `clojure_eval`, test edge cases, then save code.
- Reload namespaces with `:reload`; switch into the namespace you're working on.
- Dev loop: `(galt.main/start-system! :dev)` to start, `(go!)` to hot-reload.

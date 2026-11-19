# PROGRESS.md

## Current Verified State
- Latest commit: `b64de46` Let /events/new save with Publish at left blank
- Tests: full `test/` suite 145 tests, 367 assertions, 2 failures (both pre-existing world-map placeables, anon restricted list). Connect search namespaces 62 tests, 99 assertions, 0 failures. First events tests: 6 tests, 14 assertions, 0 failures.
- Highest priority unfinished work: none. Connect search (tickets 01-05) and the `/events/new` "Publish at" bug are done.
- Current blocker: none

## In Progress
- [ ] Coolify deployment: `Dockerfile`, `docker-entrypoint.sh`, `/health`, asset-store env overrides and `.github/workflows/deploy.yml` are written. The uberjar build was verified locally; the image build and the first Coolify deploy are not.

## Completed (recent)
- [x] `/groups` list renders group avatars: first render now resolves the storage key via `:asset-url` (uncommitted)
- [x] `/events/new` saves with "Publish at" left blank: blank-safe `parse-form-datetime` and the column default applying (`b64de46`)
- [x] Ticket 05: Near me steps aside for one retry when a query has no local Hits, and the notice survives paging (`3bc6fc4`)
- [x] Ticket 04: typo on a name or title via pg_trgm, with `:title` > `:title-fuzzy` > `:body` tiers (`d154559`)
- [x] Ticket 03: keyword search via Postgres FTS, ranked by match quality (`6b82349`)
- [x] Ticket 02: activity_at computed with grouped joins, deleted Members excluded (`8a222c3`)
- [x] Ticket 01: search result carries a snippet and a Near me notice (`d33d2a8`)
- [x] Agent skills config: local markdown tracker, default triage labels, single-context domain docs (`docs/agents/`, `AGENTS.md`)

## Next Steps
1. Nothing queued.

## Session Log
- 2026-10-25: Prepared Coolify deployment for staging (`master` -> `staging.galt.is`) and production (`production` -> `galt.is`). Image builds globo's uncommitted JS from the pinned globo SHA, packs the uberjar with `GLOBO_LOCAL_ROOT=/globo`, bakes `world.sql` (pinned commit) and the DB-IP mmdb, and runs migrations from the entrypoint before starting the server. GitHub Actions pushes `ghcr.io/galt-network/galt:sha-<sha>` and pins that tag on the Coolify app before deploying (Coolify does not re-pull mutable tags). Verified: `GLOBO_LOCAL_ROOT=... clojure -T:build uber` produces a jar containing globo namespaces and `public/3d`; `galt.core.system` and routes namespaces load. Not verified: Docker image build (no Docker here) and Coolify/GHCR setup.
- 2026-10-21: Fixed broken group avatars on `/groups`. Root cause: `list-groups` built its rows in two places, and only the Datastar/SSE branch (infinite scroll) applied `:asset-url`; the first render emitted the raw storage key (`public/<sha>.webp`), which the browser resolved relative to `/groups` and 404'd. Both branches now share a private `group-rows` helper that applies `:asset-url`; the leftover debug `println` in the SSE branch is gone. Verified live with curl on `:8081/groups`: src is now `https://media.dev.galt.is/public/<sha>.webp` (200). No tests in the groups module. Uncommitted; the deps.edn / AGENTS.md diffs in the tree are not from this change.
- 2026-10-16: Fixed `/events/new` failing to save with "Publish at" left blank (`b64de46`). Root cause: `th/parse-form-datetime` was `(jt/local-date-time s)`, which threw on `""` (and on `nil`), and a nil-valued key would have inserted an explicit NULL overriding the column default. Now blank input parses to nil and `create` omits only a blank `:publish-at`, so `publish_at default current_timestamp` applies; a blank `:end-time` stays an explicit NULL rather than becoming insert time. Review caught the first, over-broad nil strip. First tests in the events module.
- 2026-10-15: Implemented Connect search ticket 05 (`3bc6fc4`). The use case searches once more without the location when a non-blank query with a location yields no Hits, and reports `:near-me-dropped?` even when the retry is empty. No handler change was needed. The drop is decided on the ranked set before paging. This closes the Connect search spec: tickets 01-05 all done.
- 2026-10-15: Implemented Connect search ticket 04 (`d154559`). A typo path for query words of length >= 5 uses the pg_trgm `<%` operator against names/titles only (edit distance in the in-memory stand-in); ranking gained a `:title` / `:title-fuzzy` / `:body` middle tier. The text predicate became an AND of per-word ORs, with a whole-query lexeme guard so a stopword-only query matches nothing. Prefix matching deliberately skipped. Verified live: `{:query "the"}` returns nothing and `{:query "bitcoin"}` still returns the events; hyphenated and apostrophe names tokenize and match on both the FTS and trigram paths.
- 2026-10-15: Implemented Connect search ticket 03 (`6b82349`). Postgres FTS replaces substring ILIKE; the use case ranks title matches above body matches and breaks ties on activity_at; the 200-row pre-rank cap is gone; the migration adds pg_trgm, four GIN full-text indexes, and four trigram indexes. CONTEXT.md's activity_at tie-break wording is now true and shipped with this ticket. Verified live: `EXPLAIN` shows the FTS indexes are used, and `{:query "bitcoin"}` returns the events with `:match :title` and clean snippets.
- 2026-10-15: Implemented Connect search ticket 01 (`d33d2a8`). Result is `[:ok {:hits :near-me-dropped?}]`. Page renders a snippet and "Near me was dropped" only when the result sets them. CONTEXT.md activity_at wording left uncommitted; it describes ticket 03, not this slice.
- 2026-10-15: Set up Matt Pocock skill config. Issue tracker is local markdown under `.scratch/`. No tests.
- 2026-10-14: Updated `PROJECT_SUMMARY.md` from the repo (no tests, no REPL). Did not rotate credentials found in config EDNs.

# GALT - Project Summary

## Overview

**GALT** (Global Alliance for Libertarian Transformation) is a web application designed to help freedom-minded people find each other and communicate. It provides features for member management, group creation, event organization, Bitcoin Lightning payments, invitations, posts, comments, and location-based discovery.

Key principles:
- Optional anonymity
- Decentralization roadmap (IPFS, Nostr integration planned)
- Bitcoin Lightning for group fundraising (no custody)
- Open source and uncensorable
- Group-based self-moderation (no super-admin)

**Tech Stack**: Clojure backend with Ring/Reitit HTTP, PostgreSQL database, Datastar for HTMX-like reactive UI, Hiccup for HTML templating.

---

## Key File Paths

### Entry Points
- `src/galt/main.clj` - Application entry point. CLI: `init` (setup DB), `migrate` (run migrations), `assets export-public [out-dir]` (dump public assets; default dir `assets-export`), no args (start web server). REPL: `(galt.main/start-system!)`. Holds `running-system` atom and `start-system!`/`stop-system!`/`before-ns-unload`/`after-ns-reload` helpers for hot reload.
- `build.clj` - Build script using `tools.build`. Creates uberjar: `clj -T:build uber`
- `start-nrepl.sh` - Convenience script to start an nREPL server. Pins an older nREPL than `deps.edn`; prefer `clojure -M:nrepl` or `clojure -M:dev:repl -m nrepl.cmdline`.
- `start-ssh-tunnel.sh` - Convenience script for SSH tunnel to dev DB
- `Dockerfile` / `docker-entrypoint.sh` - Container image: builds globo's JS from the pinned public repo, packs the uberjar, bakes `world.sql` + GeoIP mmdb, runs migrations, then starts the server. `ENV GLOBO_LOCAL_ROOT=/globo` makes `build.clj` use the checkout with compiled JS.
- `.github/workflows/deploy.yml` - On `master`/`production`: pushes `ghcr.io/galt-network/galt:sha-<sha>` to GHCR, pins that tag on the matching Coolify app, then triggers deployment.
- `scripts/btcpay.bb` - Local regtest BTCPay Server plus a payer CLN. See `docs/btcpay.md`. Not started by clj-contain.
- `scripts/download-geoip.sh` - Downloads the DB-IP city-lite MMDB (CC-BY 4.0). Do not commit the MMDB. Dest: `GALT_GEOIP_MMDB` or `data/geoip/dbip-city-lite.mmdb`.
- `scripts/lnurl-login.bb` / `scripts/lnurl-login.edn` - LNURL login scratch script.
- `CONTEXT.md` - Domain language. Read it before naming things: Member ≠ User, Connect returns Hits, `activity_at` orders Hits, Near me is 100 km.

### System Configuration
- `src/galt/core/system.clj` - Donut System dependency graph. Defines all components: DB, repositories, use cases, routes, web server. ~780 lines, the central wiring file. `get-config` reads an EDN file, then `env-overrides`: process env wins, then `.env`, then the EDN. Never invents a host port. Named systems `:dev` / `:staging` / `:prod` each load `config/<env>-galt.edn`.
- `config/default-config.edn` - Default config (localhost:8081, file storage)
- `config/dev-galt.edn`, `config/staging-galt.edn`, `config/prod-galt.edn` - Environment-specific configs

### Core Infrastructure (`src/galt/core/infrastructure/`)
- `database.clj` - HikariCP connection pool setup for PostgreSQL
- `db_migrations.clj` / `db_migrations/` - Migratus wrapper for DB migrations
- `asset_stores.clj` - Facade: builds a disk or S3-compatible store from `:asset-store` config and exposes `:asset-url` / `:asset-key` helpers. Keys are relative (`public/<sha256>.png`), never absolute URLs.
- `disk_asset_store.clj` - `AssetStore` on the local filesystem (`:type :disk`)
- `s3_asset_store.clj` / `s3_presign.clj` - `AssetStore` for S3-compatible backends (R2, B2, MinIO). Keys fall back to `R2_ACCESS_KEY_ID` / `R2_SECRET_ACCESS_KEY`.
- `assets_export.clj` - `assets export-public` CLI implementation
- `name_generator.clj` - Anonymous name generation
- `logging.clj` - Logging configuration (Telemere setup)
- `version.clj` - Application version constant
- `bitcoin/` - Bitcoin/LNURL utilities (bouncy-castle signature verify, lnurl helpers)
- `web/` - HTTP layer:
  - `routes.clj` - Core routes (home, `/files`, `/private-assets`, `/assets`, Datastar SSE) and middleware stack
  - `middleware.clj` - Auth middleware (role-based: admin/member/user), session management, request logger. Route data key is `:min-role`.
  - `server.clj` - http-kit server start/stop
  - `helpers.clj` - Misc web helpers
  - `db_session_store.clj` - Unused PostgreSQL `SessionStore`. The running system uses Ring's in-memory store over the `:galt-session` atom. Do not wire this without a `session_storage` migration.
  - `sse_connection_store.clj` - In-memory store for Datastar SSE connections

### Domain Modules (Clean Architecture)
Each module follows the pattern: `adapters/`, `domain/` (with `entities/` and `use_cases/`), `external/`.

- `src/galt/groups/` - Group management (create, edit, delete, list, show, memberships)
- `src/galt/members/` - Member profiles, LNURL authentication, search
- `src/galt/invitations/` - Invitation system (requests, creation, dashboard)
- `src/galt/payments/` - Lightning membership payments via BTCPay Greenfield (`BTC-LN`). `scripts/btcpay.bb` is the local regtest stack.
- `src/galt/posts/` - Posts within groups (CRUD)
- `src/galt/events/` - Event management (add, list)
- `src/galt/comments/` - Comments on posts and events (has `adapters/presentation.clj` instead of a `presentation/` dir)
- `src/galt/locations/` - Geographic locations (city, country, geocoding). Also `adapters/geoip.clj` + `adapters/client_ip.clj`: DB-IP MMDB lookup used as `:infer-location` on route deps (member location, else IP city; loopback uses the machine's public IP). Not a Donut component.
- `src/galt/connect/` - Mixed search feed at `/connect`. Hits are Members, Groups, Events, and Posts, ranked in `domain/rank.clj`. `domain/use_cases/search.clj` is called from the handler with route deps (`:search-repo`, `:infer-location`); it is **not** a Donut `:use-cases` component and returns `[:ok hits]`, not the usual 3-tuple. Repo: `adapters/db_search_repository.clj` (wired as `:storage :search`). `adapters/memory_search_repository.clj` is the in-memory stand-in for tests.
- `src/galt/design/` - UI/design routes (only `handlers.clj` + `routes.clj`, no full module structure)
- `src/galt/world_map/` - World map page (full-bleed Globo 3D map, SSE connection, session-tied identity, sliding auto-hide navbar; mounted under `:globo-mount-path`). Files: `adapters/handlers.clj`, `adapters/placeables.clj` (member pins on map), `adapters/presentation.clj`, `domain/use_cases/publish_placeables.clj`, `domain/use_cases/send_globo_message.clj`, `external/routes.clj`. Routes: page, `/connection`, `/send-message`, `/assets/*path`.
- `src/galt/shared/` - Shared cross-cutting presentation helpers (`presentation/translations.clj` for i18n strings; strings live in `resources/translations/en.edn`)

### Domain Layer Pattern (per module)
- `domain/entities/*.clj` - Pure data records with `clojure.spec` validation
- `domain/*_repository.clj` - Protocol definitions (interfaces)
- `domain/use_cases/*.clj` - Business logic, pure functions
- `adapters/*_repository.clj` - Protocol implementations (DB access via HoneySQL)
- `adapters/handlers.clj` - HTTP request handlers
- `adapters/presentation/*.clj` (newer modules) or `adapters/views.clj` / `adapters/view_models.clj` (older modules) - HTML rendering with Hiccup
- `external/routes.clj` - Reitit route definitions

### Adapters & Shared
- `src/galt/core/domain/asset_store.clj` - `AssetStore` protocol (put/get/delete/presign/list). No Ring types in the protocol except `get-content-response`.
- `src/galt/core/domain/asset_repository.clj` + `src/galt/core/adapters/db_asset_repository.clj` - Asset row persistence (`:storage :asset`)
- `src/galt/core/domain/entities/asset.clj` - Asset entity
- `src/galt/core/domain/use_cases/upload_asset.clj` - Upload use case (dedup by content + visibility)
- `src/galt/core/domain/use_cases/landing_page.clj` - Landing-page counts and recent items
- `src/galt/core/adapters/handlers.clj` - Home, Datastar SSE, `/files`, `/private-assets`
- `src/galt/core/adapters/db-access.clj` - DB access abstraction
- `src/galt/core/adapters/postgres-db-access.clj` - PostgreSQL implementation
- `src/galt/core/adapters/sse-helpers.clj` - Server-Sent Events utilities
- `src/galt/core/adapters/url-helpers.clj` - URL manipulation
- `src/galt/core/adapters/db-result-transformations.clj` - DB row transformation
- `src/galt/core/adapters/link-generator.clj` - Link/URL generation
- `src/galt/core/adapters/time-helpers.clj` - Time utilities
- `src/galt/core/adapters/number-helpers.clj` - Numeric helpers
- `src/galt/core/adapters/presentation-helpers.clj` - Cross-cutting presentation helpers
- `src/galt/core/adapters/view-models.clj` - Shared view model helpers
- `src/galt/core/seed.clj` - Dev/test DB seeding
- `src/galt/core/views/layout.clj` - Base HTML layout
- `src/galt/core/views/components.clj` & `components/` - Reusable Hiccup view components
- `src/galt/core/views/datastar_helpers.clj` - Datastar signal helpers
- `src/galt/core/views/landing_page.clj` - Landing page view
- `src/galt/core/views/table.clj` - Table-rendering helper

### Database Migrations
- `resources/migrations/` - SQL migration files (Migratus format, `YYYYMMDDHHMMSS-name.{up,down}.sql`)
- `resources/migrations/init.sql` - Initial DB setup
- Current migrations in order:
  - `20250814104102-create-users` - users table
  - `20250821152657-create-groups` - groups table
  - `20250830152849-create-memberships` - memberships table
  - `20250904143744-import-cities-countries.edn` - EDN data import for geodata (uses `GALT_CITIES_COUNTRIES_SQL_PATH`)
  - `20250905193629-create-locations` - locations table
  - `20250906190631-create-invitations` - invitations table
  - `20250915223524-create-invoices-invitation-usages` - invoices + invitation usages
  - `20250926215144-create-posts` - posts table
  - `20250929005538-create-events` - events table
  - `20251001194743-create-comments` - comments table (used for both posts and events)
  - `20260810120000-create-assets` - `assets` table; `asset_ids` jsonb on posts and events
  - `20260810120010-rewrite-avatar-urls-to-keys` - member/group avatars from `/files/` URLs to storage keys

### Tests
- `test/galt/` - Test files mirroring src structure (covers `connect`, `core`, `groups`, `invitations`, `locations`, `members`, `payments`, `posts`, `world_map`)
- Test libraries: `matcher-combinators`, `ring-mock`, `http-kit.fake`, `spy`

### Dev/REPL
- `dev/user.clj` - REPL user namespace with `clj-reload` for hot reloading. `(go!)` to reload, `(galt.main/start-system! :dev)` to start. Sets `*warn-on-reflection* = true`. Includes `sci.nrepl.browser-server` setup for browser REPL.
- `dev/lnurl_experiments.clj` - LNURL experimentation scratchpad

### Documentation
- `CONTEXT.md` - Domain language (not a design doc)
- `docs/architecture.md` - Clean Architecture explanation with folder structure
- `docs/development.md` - Dev setup and deployment instructions
- `docs/geocoding.md` - Geocoding setup
- `docs/assets.md` - Asset pipeline (disk vs S3/R2, keys, upload limits, CDN)
- `docs/btcpay.md` - Local regtest BTCPay + payer CLN (not started by clj-contain). Its opening line ("Galt does not talk to BTCPay yet") is stale: the app has a Greenfield gateway and a webhook.
- `docs/integration-with-cloudflare.md` - R2 bucket + custom domain setup. Do not print `config/*.edn` or `.env` (credentials).
- `docs/ideas.md` - Roadmap / ideas scratchpad (untracked)

---

## Dependencies

Versions reflect the current `deps.edn`.

### Web & HTTP
| Dependency | Version | Role |
|------------|---------|------|
| `http-kit/http-kit` | 2.9.0-beta3 | Async HTTP server |
| `metosin/reitit` | 0.10.1 | Routing (data-driven, composable) |
| `metosin/ring-http-response` | 0.9.5 | HTTP response helpers |
| `ring-cors/ring-cors` | 0.1.13 | CORS middleware |
| `com.github.seancorfield/ring-data-json` | 0.5.3 | JSON ring middleware |
| `ring-logger/ring-logger` | 1.1.1 | Request logging |

### Frontend
| Dependency | Version | Role |
|------------|---------|------|
| `dev.data-star.clojure/sdk` | 1.0.0-RC11 | Datastar (HTMX-like reactive updates via SSE) |
| `dev.data-star.clojure/ring` | 1.0.0-RC11 | Datastar Ring integration |
| `dev.data-star.clojure/http-kit` | 1.0.0-RC11 | Datastar http-kit adapter |
| `hiccup/hiccup` | 2.0.0 | HTML templating (Clojure vectors -> HTML) |
| `markdown-clj/markdown-clj` | 1.12.8 | Markdown parsing |
| `is.mad/globo` | local root `../globo` | 3D world map UI (Globo) - served under `:globo-mount-path`; local path dependency, not from Maven |

### System & Architecture
| Dependency | Version | Role |
|------------|---------|------|
| `party.donut/system` | 1.0.259 | Dependency injection / component lifecycle |
| `org.clojure/core.async` | 1.9.865 | CSP-style concurrency |
| `org.clojure/core.match` | 1.1.1 | Pattern matching |
| `org.clojure/spec.alpha` | 0.6.249 | Data specification & validation |
| `failjure/failjure` | 2.3.0 | Error handling monads |

### Database
| Dependency | Version | Role |
|------------|---------|------|
| `com.github.seancorfield/next.jdbc` | 1.3.1118 | JDBC wrapper |
| `com.github.seancorfield/honeysql` | 2.7.1389 | SQL as Clojure data structures |
| `com.zaxxer/HikariCP` | 7.1.0 | Connection pooling |
| `org.postgresql/postgresql` | 42.7.11 | PostgreSQL JDBC driver |
| `migratus/migratus` | 1.6.6 | Database migrations |

### Bitcoin & Crypto
| Dependency | Version | Role |
|------------|---------|------|
| `org.bitcoinj/bitcoinj-core` | 0.17.1 | Bitcoin protocol library |
| `org.bouncycastle/bcprov-jdk18on` | 1.84 | Cryptographic provider |
| `buddy/buddy` | 2.0.0 | Cryptography & auth utilities |
| `clj.qrgen/clj.qrgen` | 0.4.0 | QR code generation |

### Utilities
| Dependency | Version | Role |
|------------|---------|------|
| `clojure.java-time/clojure.java-time` | 1.4.3 | Java Time API wrapper |
| `com.cnuernber/charred` | 1.039 | Fast JSON/EDN parsing |
| `camel-snake-kebab/camel-snake-kebab` | 0.4.3 | Case conversion (kebab->snake, etc.) |
| `lambdaisland/uri` | 1.19.155 | URI parsing/manipulation |
| `clj-fuzzy/clj-fuzzy` | 0.4.1 | Fuzzy string matching |
| `danlentz/clj-uuid` | 0.2.5 | UUID generation (v7 for lexical ordering) |
| `com.maxmind.geoip2/geoip2` | 4.3.1 | DB-IP / MaxMind MMDB city lookup (Connect "near me") |
| `com.cognitect.aws/api` | 0.8.838 | AWS API client (S3-compatible asset store) |
| `com.cognitect.aws/endpoints` | 871.2.50.3 | AWS endpoint data |
| `com.cognitect.aws/s3` | 871.2.49.4 | S3 API (R2, B2, MinIO) |

### Logging & Telemetry
| Dependency | Version | Role |
|------------|---------|------|
| `com.taoensso/telemere` | 1.2.1 | Structured logging |

### Dev/REPL Tools
| Dependency | Version | Role |
|------------|---------|------|
| `nrepl/nrepl` | 1.7.0 | nREPL server |
| `cider/cider-nrepl` | 0.59.0 | CIDER middleware |
| `io.github.babashka/sci.nrepl` | 0.0.2 | SCI-based nREPL (browser REPL) |
| `io.github.tonsky/clj-reload` | 1.0.0 | Hot namespace reloading |
| `org.clojure/clojure` | 1.12.5 | Clojure (in `:repl` alias) |
| `tortue/spy` | 2.15.0 | Debug logging (like `tap>` but visible) |
| `ring/ring-mock` | 0.6.2 | Test request mocking |
| `http-kit.fake/http-kit.fake` | 0.2.2 | HTTP client mocking |
| `nubank/matcher-combinators` | 3.10.0 | Test data matching |

### Deps.edn Aliases
- `:repl` - Adds `dev/` path, Clojure 1.12.5, `clj-reload`
- `:dev` - Adds `test/` path, spy, ring-mock, http-kit.fake, matcher-combinators
- `:nrepl` - `dev/` + `test/` paths and the `:dev` test libs; starts `nrepl.cmdline` (`clojure -M:nrepl`)
- `:build` - `tools.build` 0.10.14 for uberjar via `clj -T:build uber`
- `:outdated` - `antq` for dependency update checks (`clj -M:outdated`)
- `:clj-contain` - Containerized dev app (`galt-dev`, port 8081) plus a Postgres 18 service, joined to the `galt-btcpay_default` network. Not a code alias. See `deps.edn`.

---

## Architecture

### Clean Architecture Layers

```
External (frameworks, DB, web)
    ↓
Adapters (transform data between external and domain)
    ↓
Use Cases (business rules, orchestration)
    ↓
Entities (pure data + functions, no external dependencies)
```

### Component Dependency Graph (Donut System)

The system is organized into tiers:

1. **`:env`** - Config values (`:galt-root-url`, `:asset-store`, `:globo-mount-path`, `:geoip-mmdb`, BTCPay settings)
2. **`:storage`** - Database connection, repositories (group, member, user, location, invitation, payment, post, event, comment, search, asset), `:file-storage` (asset-store facade), in-memory session store, Globo SSE + placeables
3. **`:gateways`** - External service gateways (BTCPay payment gateway)
4. **`:use-cases`** - Business logic functions wired with repository/gateway dependencies. Connect search is not in this tier.
5. **`:app`** - Route dependencies (includes `:infer-location`, `:asset-url`, `:search-repo`), individual routers per module (including `:connect/routes`), merged router, route handler, web server

Components reference each other via `(ds/ref [:tier :component-name])`.

### Request Flow

```
HTTP Request → http-kit server → middleware stack → Reitit router → handler function → use case → repository protocol → DB adapter → PostgreSQL
                                                                        ↓
HTML Response ← Hiccup rendering ← view model ← use case result ← entity transformation ← DB row
```

### Middleware Stack (outer to inner)
1. `wrap-session` - Session management
2. `wrap-update-related-session` - Cross-session updates
3. `wrap-multipart-params` - File upload parsing
4. `wrap-params` - Query/form params
5. `wrap-keyword-params` - String keys -> keywords
6. `wrap-method-override` - POST _method param for PUT/DELETE/PATCH
7. `wrap-with-logger` - Request logging via Telemere
8. `wrap-auth` - Role-based authentication (admin/member/user/nil)

---

## Implementation Patterns

### Repository Pattern
- **Protocol** defined in `domain/*_repository.clj` (pure interface)
- **Implementation** in `adapters/db_*_repository.clj` (record implementing protocol)
- **Constructor** function `new-*-repository` returns record instance
- Uses HoneySQL for queries, `transform-row` for DB row -> entity conversion

Example:
```clojure
;; Protocol (domain)
(defprotocol GroupRepository
  (add-group [this creator-id group])
  (find-group-by-id [this group-id]))

;; Implementation (adapter)
(defrecord DbGroupRepository [db-access]
  GroupRepository
  (find-group-by-id [_ id]
    (->> {:select [:*] :from [:groups] :where [:= :id id]}
         (query db-access)
         first
         (transform-row group-spec)
         (map->Group))))
```

### Use Case Pattern
- Pure functions receiving dependencies as a map
- Return `[:ok result nil]` or `[:error nil errors]` tuples (failjure-style)
- Validation via `clojure.spec` and custom predicate lists
- Wired in `system.clj` with `partial` to inject dependencies

Example:
```clojure
(defn add-group-use-case [deps command]
  (s/assert ::command command)
  (let [validation-errors (validate-all deps requirements command)]
    (if (empty? validation-errors)
      [:ok (create-group deps command) nil]
      [:error nil validation-errors])))
```

### Entity Pattern
- `defrecord` for data structure
- `clojure.spec` for validation
- Constructor function with `:post` condition for spec assertion

Example:
```clojure
(s/def ::id uuid?)
(s/def ::name (s/and string? #(>= (count %) 5)))
(s/def ::group (s/keys :req-un [::id ::name ::avatar ::description ::created-at]))

(defrecord Group [id name description created-at location-id])

(defn new-group [{:keys [id name description avatar created-at]}]
  {:post [#(s/assert ::group %)]}
  (map->Group {:id id :name name :description description :avatar avatar :created-at created-at}))
```

### Dependency Injection via Donut System
- Components defined as maps with `::ds/start`, `::ds/stop`, `::ds/config`
- References via `(ds/ref [:tier :name])`
- Use cases created with `partial` to pre-bind dependencies
- System started/stopped via `(ds/signal system ::ds/start)` and `::ds/stop`

### Web Routes
- Reitit data-driven routing
- Each module has `external/routes.clj` with `(defn router [deps] ...)`
- Core routes merge all module routers via `merge-routers`
- Handlers receive `deps` map with repositories, use cases, rendering functions

### Datastar (Reactive UI)
- Server-Sent Events for reactive updates
- `with-sse` helper for SSE responses
- `datastar-sse` endpoint for persistent SSE connection
- Signal-based state updates from server
- Attribute delimiters use `:` (colon), not `-` (dash) - updated in commit `2e809b3` to match new Datastar convention. E.g. use `data-bind:foo` not `data-bind-foo`

---

## APIs & Functions

### REPL Functions
```clojure
;; Start system
(galt.main/start-system!)           ; Uses GALT_ENV env var
(galt.main/start-system! :dev)      ; Explicit environment

;; Access running system components
@galt.main/running-system           ; Atom containing system map
(get-in @running-system [:donut.system/instances :storage :group])

;; Reload code (from user namespace)
(go!)                               ; clj-reload reload

;; Common repository operations
(require '[galt.groups.domain.group-repository :refer [list-groups find-groups-by-member]])
(def repo (get-in @running-system [:donut.system/instances :storage :group]))
(find-groups-by-member repo 42)
```

### CLI Commands
```bash
# Initialize database (first time only)
java -jar galt-0.1.X-standalone.jar init

# Run migrations
java -jar galt-0.1.X-standalone.jar migrate

# Start web server
java -jar galt-0.1.X-standalone.jar

# Export public assets
java -jar galt-0.1.X-standalone.jar assets export-public [out-dir]

# Build uberjar
clj -T:build uber

# Start REPL
clojure -M:dev:repl -m nrepl.cmdline
clojure -M:nrepl
clojure -M:dev:repl -m galt.main
```

### Key Use Cases (available via system)
- `:create-invitation-use-case` - Create new invitation
- `:create-invitation-request-use-case` - Request invitation
- `:invitation-dashboard-use-case` - Show invitation dashboard
- `:add-group-use-case` - Create a new group
- `:new-group-use-case` - New group form data
- `:show-group-use-case` - Display group details
- `:list-groups-use-case` - List/search groups
- `:edit-group-use-case` - Edit group form
- `:update-group-use-case` - Update group
- `:delete-group-use-case` - Delete group
- `:show-profile-use-case` - Show member profile
- `:search-members-use-case` - Search members
- `:start-lnurl-login-use-case` - Start LNURL auth flow
- `:complete-lnurl-login-use-case` - Complete LNURL auth
- `:watch-lnurl-login-use-case` - Watch for LNURL auth completion
- `:create-member-use-case` - Create member profile
- `:update-member-use-case` - Update member profile
- `:membership-payment-use-case` - Create membership payment invoice
- `:update-invoice-use-case` - Update invoice status
- `:create-post-use-case` - Create post
- `:update-post-use-case` - Update post
- `:delete-post-use-case` - Delete post
- `:add-event-use-case` - Add event
- `:list-events-use-case` - List events
- `:publish-placeables-use-case` - Publish Globo map pins
- `:send-globo-message-use-case` - Send a Globo chat message
- `:landing-page-use-case` - Landing page counts and recent items
- `:upload-asset-use-case` - Store an uploaded file (dedup by hash + visibility)

Connect search is `galt.connect.domain.use-cases.search/search`, called from `galt.connect.adapters.handlers/show-connect`. It is not in the system map.

---

## Development Workflow

### Starting Development
1. Start REPL: `clojure -M:dev:repl -m nrepl.cmdline` (or `./start-nrepl.sh`)
2. Connect from editor (Conjure: `:ConjureConnect`)
3. Start system: `(galt.main/start-system! :dev)`
4. Open http://localhost:8081
5. Edit code, then `(go!)` to reload namespaces (via `clj-reload`)
6. Optional browser REPL: `(require '[sci.nrepl.browser-server :as nrepl]) (nrepl/start! {:nrepl-port 1339 :websocket-port 1340})`

### Hot Reload Hooks
`galt.main` exposes `before-ns-unload` (calls `stop-system!`) and `after-ns-reload` (calls `start-system!`) so `clj-reload` brings the system down/up automatically between reloads.

### Environment Variables
- `GALT_ENV` - No-arg `(start-system!)` becomes `(start-system! (keyword GALT_ENV))`. `:dev` / `:staging` / `:prod` each load `config/<env>-galt.edn`. Hot reload calls the no-arg version, so it follows `GALT_ENV`, not a keyword passed earlier.
- `GALT_CONFIG` - Only the no-arg `(get-config)` (assets export, ad-hoc reads). Default `config/default-config.edn`. The running server does not use this; it uses the named-system file.
- `MIGRATUS_DATABASE`, `MIGRATUS_USER`, `MIGRATUS_PASSWORD` - DB credentials
- `DB_HOST`, `DB_PORT` - Postgres host/port (default `localhost:5432`)
- `APP_PORT` - Overrides `:web-server-port`. Process env only, not `.env`.
- `APP_HOST_PORT`, `GALT_ROOT_URL` - Override `:galt-root-url` (`APP_HOST_PORT` becomes `http://127.0.0.1:<port>`). `GALT_ROOT_URL` also overrides `[:asset-store :root-url]` (used for private asset links)
- `GALT_ASSET_STORE_BUCKET`, `GALT_ASSET_STORE_ENDPOINT`, `GALT_ASSET_STORE_PUBLIC_BASE_URL` - Override the matching `:asset-store` fields (containers keep bucket names out of the image)
- `BTCPAY_URL`, `BTCPAY_API_KEY`, `BTCPAY_STORE_ID`, `BTCPAY_WEBHOOK_SECRET` - Override BTCPay config
- `R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY` - S3/R2 credentials when config keys are nil
- `GALT_GEOIP_MMDB` - MMDB path. Process env only; else config `:geoip-mmdb`, else `data/geoip/dbip-city-lite.mmdb`.
- `GALT_CITIES_COUNTRIES_SQL_PATH` - Path to world.sql for geodata import
- `.env` - Override file. Precedence is process env, then `.env`, then EDN. Do not print it or `config/*.edn`.

### Config Files
- `config/default-config.edn` - Fallback for no-arg `(get-config)` only. `:asset-store :type :disk`.
- `config/dev-galt.edn` - Loaded by the `:dev` named system
- `config/staging-galt.edn` - Loaded by the `:staging` named system
- `config/prod-galt.edn` - Loaded by the `:prod` named system
- `:globo-mount-path` - Globo map mount path (default `"/world-map"`), present in all config EDNs
- `:asset-store` - `:type :disk` (default config) or `:s3` (dev/staging/prod configs). See `docs/assets.md`. Do not copy credentials out of these files.
- `:geoip-mmdb` - Path to the city MMDB

### Testing
There is no `:test` alias. Test sources and `matcher-combinators` / `ring-mock` / `http-kit.fake` are on the `:dev` classpath. Run tests from the REPL (`clojure -M:dev:repl` or `clojure -M:nrepl`).

---

## Extension Points

### Adding a New Domain Module
1. Create `src/galt/<module>/` with subdirectories:
   - `domain/entities/` - Entity records + specs
   - `domain/` - Repository protocol
   - `domain/use_cases/` - Use case functions
   - `adapters/` - Repository implementation, handlers, views
   - `external/routes.clj` - Route definitions
2. Add repository to `system.clj` under `:storage`
3. Add use cases to `system.clj` under `:use-cases`
4. Add routes to `system.clj` under `:app` and merge in `:router`
5. Create tests in `test/galt/<module>/`

### Adding a New Use Case
1. Create `src/galt/<module>/domain/use_cases/<use_case>.clj`
2. Define function taking `[deps command]` map
3. Add to `system.clj` under `:use-cases` with dependency wiring
4. Reference from handlers/routes

### Adding External Integrations
1. Create gateway in `src/galt/<module>/external/` or `src/galt/core/infrastructure/`
2. Define protocol in `domain/`
3. Implement in `adapters/`
4. Add to `system.clj` under `:gateways` or `:storage`

### Database Changes
1. Create migration in `resources/migrations/` (Migratus naming convention)
2. Run `java -jar galt-0.1.X-standalone.jar migrate` (or `(db-migrations/migrate!)` from the REPL)

### UI/Frontend
- Hiccup templates in `adapters/views.clj` or `core/views/`
- Datastar signals for reactive updates
- SSE endpoint at `/datastar-sse` for persistent connections
- Static assets served from `/assets/*` (resources/public/)
- World map assets: `resources/public/js/world-map.js` + `resources/public/css/world-map.css` (sliding auto-hide navbar); Globo UI compiled JS/CSS/3D models served from `/world-map/assets/*` via globo's classpath resources
- User media: `POST`/`PUT /files` uploads; public objects are served from `:public-base-url` + key; private objects via `GET /private-assets/*` (`:min-role :user`). Avatars and other media store keys, not absolute URLs.

---

## Conventions

- **Naming**: kebab-case for namespaces, functions, keywords; PascalCase for records/types
- **Files**: snake_case filenames matching namespace segments
- **Use cases**: return `[:ok result nil]` or `[:error nil errors]`
- **Repositories**: protocol in domain, defrecord implementation in adapters
- **Specs**: `clojure.spec.alpha` for entity validation
- **DB queries**: HoneySQL data structures, not raw SQL
- **Dependencies**: injected via Donut System, never required directly in domain layer
- **Pure domain**: no external dependencies in `domain/` namespaces
- **Session keys**: `:user-id`, `:member-id`, `:admin` for auth state
- **Comments**: shared by both posts and events (extracted in commit `bc14af2`); `src/galt/comments/` is the canonical module, posts/events reuse it
- **Members search**: infinite scroll via Datastar SSE (commit `4951d03`), not pagination
- **Membership validity**: considers payment date, not just invoice status (commit `f712c45`)
- **Connect**: one feed of Hits. Language is in `CONTEXT.md`. Search is handler-called, not a Donut use case, and returns `[:ok hits]`.
- **Assets**: relative storage keys in the DB; URLs composed at render time via `:asset-url`. Same bytes + same visibility reuse one row.
- **Secrets**: `config/*.edn` and `.env` may hold live credentials. Do not print them.

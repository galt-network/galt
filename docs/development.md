## Development

BTCPay regtest (Lightning invoices, both containerized and host Galt): [btcpay.md](btcpay.md).

Both ways of running Galt need a JDK and the Clojure CLI.

### On the operating system (new checkouts, agents)

Needs git and docker or podman as well. Nothing else has to be set up on the machine.

1. `./scripts/dev.sh up` starts a Postgres container, creates the database and runs the migrations.
   - The first run imports the bundled geodata sample (`resources/geodata/`). `./scripts/dev.sh geodata` downloads the full dataset and starts over; `./scripts/dev.sh geoip` downloads the GeoIP database (optional).
2. `./scripts/dev.sh run` starts the app at http://localhost:8081.
   - With `GALT_ENV` unset it runs the `:local` system: `config/default-config.edn`, files stored under `resources/uploads`.
3. `clojure -M:test` runs every test. GitHub Actions runs it, and `./scripts/dev.sh up` followed by a health check, on every pull request (`.github/workflows/test.yml`).

For a REPL run `./scripts/dev.sh up`, then `clojure -M:nrepl`, evaluate `(galt.main/start-system!)` and open http://localhost:8081. `(user/go!)` reloads changed code.

`./scripts/dev.sh down` stops Postgres and keeps the data. `./scripts/dev.sh reset` drops the data and starts again. `./scripts/dev.sh` lists the commands.

### In the clj-contain container (manual development)

`clj-contain up` starts the `galt-dev` container from the `:clj-contain` alias in `deps.edn`, with its own Postgres, the full `data/countries-states-cities-database/world.sql` and the `:dev` configuration (`config/dev-galt.edn`, files stored in the shared dev R2 bucket). The host publishes the app on port 13003.

## Tailwind (new design)

Tailwind CSS v4 compiles `resources/css/tailwind.css` to `resources/public/css/tailwind.css`. The generated file is not committed. Design reference: `docs/design/learn/`.

- Build once: `npm install && npm run css`
- Rebuild on change: `npm run css:watch`

## Release & deployment

A push to `master` or `production` runs `.github/workflows/deploy.yml`: it builds the Docker image (`Dockerfile`: uberjar, the pinned `world.sql` and a GeoIP database), pushes it to GHCR and tells Coolify to deploy it. `docker-entrypoint.sh` waits for Postgres, runs the migrations and starts the server. Set `GALT_ENV` (`staging` or `prod`) and `GALT_CITIES_COUNTRIES_SQL_PATH` (the full `world.sql`); it refuses to start without them. `MIGRATUS_*` and `DB_HOST` point it at the database.

To run a release by hand: `clj -T:build uber`, then `java -jar target/galt-*-standalone.jar migrate` and `java -jar target/galt-*-standalone.jar` with the same variables set.

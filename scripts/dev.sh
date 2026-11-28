#!/usr/bin/env bash
# Local development with a Postgres container. Needs a JDK, the Clojure CLI and
# docker or podman. Nothing else has to be set up on the machine.
#
#   up       start Postgres, create the database, run the migrations
#   run      start the app at http://localhost:8081 (system :local when GALT_ENV is unset)
#   dev      start the nREPL server (system :local when GALT_ENV is unset). (user/start-system!) to start the app
#   down     stop Postgres (the data volume is kept)
#   reset    drop the data, then up
#   geodata  download the full world.sql, then reset
#   geoip    download the GeoIP database (scripts/download-geoip.sh)
#
# A migration imports the bundled geodata sample, or
# data/countries-states-cities-database/world.sql when that file exists.
# The full dump needs an empty database: run reset after switching to it.
set -euo pipefail

cd "$(dirname "$0")/.."

world_sql=data/countries-states-cities-database/world.sql
world_sql_url=https://raw.githubusercontent.com/dr5hn/countries-states-cities-database/3f1f31dda8d758704b0ed900aaeb64268514cfce/psql/world.sql

if command -v docker >/dev/null; then
  runtime=docker
elif command -v podman >/dev/null; then
  runtime=podman
else
  echo "dev.sh needs docker or podman on PATH" >&2
  exit 1
fi

start_postgres() {
  if ! "$runtime" inspect -f '{{.State.Running}}' galt-postgres 2>/dev/null | grep -q true; then
    "$runtime" rm -f galt-postgres >/dev/null 2>&1 || true
    "$runtime" run -d --name galt-postgres -p 5432:5432 \
      -e POSTGRES_USER=galt -e POSTGRES_PASSWORD=galtpass -e POSTGRES_DB=galt_dev \
      -v galt-postgres-data:/var/lib/postgresql postgres:18-alpine >/dev/null
  fi
  for _ in $(seq 60); do
    if "$runtime" exec galt-postgres pg_isready -q -h 127.0.0.1 -U galt -d galt_dev; then
      return
    fi
    sleep 1
  done
  echo "Postgres did not become ready, check: $runtime logs galt-postgres" >&2
  exit 1
}

up() {
  start_postgres
  if [ -f "$world_sql" ]; then
    export GALT_CITIES_COUNTRIES_SQL_PATH="${GALT_CITIES_COUNTRIES_SQL_PATH:-$world_sql}"
  fi
  clojure -M -m galt.main init
  clojure -M -m galt.main migrate
}

reset() {
  "$runtime" rm -f galt-postgres 2>/dev/null || true
  "$runtime" volume rm galt-postgres-data 2>/dev/null || true
  up
}

case "${1:-}" in
  up) up ;;
  run) exec clojure -M -m galt.main ;;
  dev) clojure -M:dev:repl -m nrepl.cmdline ;;
  down) "$runtime" rm -f galt-postgres 2>/dev/null || true ;;
  reset) reset ;;
  geodata)
    [ -f "$world_sql" ] || curl -fsSL "$world_sql_url" -o "$world_sql"
    reset
    ;;
  geoip) exec scripts/download-geoip.sh ;;
  *)
    sed -n '2,/^$/p' "$0" | cut -c3-
    exit 1
    ;;
esac

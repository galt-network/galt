#!/bin/sh
# Migrations must run in the new container before it serves traffic:
# Coolify's pre-deployment command runs in the old container and is skipped
# on the first deploy.
set -eu

: "${GALT_ENV:?set GALT_ENV to staging or prod}"

db_host="${DB_HOST:-localhost}"
db_port="${DB_PORT:-5432}"

echo "Waiting for Postgres at ${db_host}:${db_port}..."
i=0
until pg_isready -q -h "$db_host" -p "$db_port" -U "${MIGRATUS_USER:-postgres}"; do
  i=$((i + 1))
  if [ "$i" -ge 60 ]; then
    echo "Postgres not ready after 120s" >&2
    exit 1
  fi
  sleep 2
done

echo "Running migrations..."
java -jar /app/galt.jar migrate

exec java ${JAVA_OPTS:--XX:MaxRAMPercentage=75} -jar /app/galt.jar

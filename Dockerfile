# syntax=docker/dockerfile:1

# Tailwind is compiled here so the generated CSS stays out of git.
FROM node:22-alpine AS css
WORKDIR /css
COPY package.json package-lock.json ./
RUN npm ci
COPY resources/css ./resources/css
COPY src ./src
RUN npm run css

FROM clojure:temurin-21-tools-deps-bookworm AS build
WORKDIR /build
COPY deps.edn build.clj ./
COPY src ./src
COPY resources ./resources
COPY --from=css /css/resources/public/css/tailwind.css resources/public/css/tailwind.css
RUN clojure -T:build uber

# Geodata is expensive to fetch, so it gets its own cached layer.
FROM eclipse-temurin:21-jre-noble AS geodata
ARG WORLD_SQL_URL=https://raw.githubusercontent.com/dr5hn/countries-states-cities-database/3f1f31dda8d758704b0ed900aaeb64268514cfce/psql/world.sql
ARG GEOIP_YM=2026-10
RUN apt-get update \
 && apt-get install -y --no-install-recommends ca-certificates curl \
 && rm -rf /var/lib/apt/lists/*
RUN mkdir -p /data/geoip \
 && curl -fsSL "$WORLD_SQL_URL" -o /data/world.sql \
 && curl -fsSL "https://download.db-ip.com/free/dbip-city-lite-${GEOIP_YM}.mmdb.gz" -o /tmp/geoip.mmdb.gz \
 && gunzip -c /tmp/geoip.mmdb.gz > /data/geoip/dbip-city-lite.mmdb \
 && rm /tmp/geoip.mmdb.gz

FROM eclipse-temurin:21-jre-noble
RUN apt-get update \
 && apt-get install -y --no-install-recommends ca-certificates curl postgresql-client \
 && rm -rf /var/lib/apt/lists/*

RUN groupadd --gid 10001 galt \
 && useradd --uid 10001 --gid galt --home-dir /app --shell /usr/sbin/nologin galt \
 && mkdir -p /app/config /data/uploads \
 && chown -R galt:galt /app /data

WORKDIR /app

COPY --from=build /build/target/galt-*-standalone.jar /app/galt.jar
COPY --chown=galt:galt config/staging-galt.edn config/prod-galt.edn /app/config/
COPY --from=geodata --chown=galt:galt /data /app/data
COPY --chown=galt:galt docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod 0555 /app/docker-entrypoint.sh

USER galt

ENV APP_PORT=8081 \
    GALT_CITIES_COUNTRIES_SQL_PATH=/app/data/world.sql \
    GALT_GEOIP_MMDB=/app/data/geoip/dbip-city-lite.mmdb \
    JAVA_OPTS="-XX:MaxRAMPercentage=75"

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=5s --start-period=900s --retries=3 \
  CMD curl -fsS http://127.0.0.1:8081/health || exit 1

ENTRYPOINT ["/app/docker-entrypoint.sh"]

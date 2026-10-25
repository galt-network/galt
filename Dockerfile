# syntax=docker/dockerfile:1

# Globo's compiled JS is not committed, so build it here and let tools.build
# pack this checkout (GLOBO_LOCAL_ROOT). Keep GLOBO_REF in sync with the globo
# pin in deps.edn.
FROM eclipse-temurin:21-jdk-noble AS globo
ARG GLOBO_REPO=https://github.com/galt-network/globo.git
ARG GLOBO_REF=2bce2db41ec3147e791acc235cace48a0f10687d
RUN apt-get update \
 && apt-get install -y --no-install-recommends ca-certificates curl git \
 && curl -fsSL https://deb.nodesource.com/setup_22.x | bash - \
 && apt-get install -y --no-install-recommends nodejs \
 && rm -rf /var/lib/apt/lists/*
RUN git init /globo \
 && cd /globo \
 && git remote add origin "$GLOBO_REPO" \
 && git fetch --depth 1 origin "$GLOBO_REF" \
 && git checkout --detach FETCH_HEAD
WORKDIR /globo
RUN npm ci --include=dev \
 && npx shadow-cljs release globo

FROM clojure:temurin-21-tools-deps-bookworm AS build
# Force the globo dep to the checkout that has the compiled JS.
ENV GLOBO_LOCAL_ROOT=/globo
WORKDIR /build
COPY deps.edn build.clj ./
COPY src ./src
COPY resources ./resources
# Only the dep root, source and resources are needed; skip node_modules.
COPY --from=globo /globo/deps.edn /globo/
COPY --from=globo /globo/src /globo/src
COPY --from=globo /globo/resources /globo/resources
RUN clojure -T:build uber

# Geodata is expensive to fetch, so it gets its own cached layer.
FROM eclipse-temurin:21-jre-noble AS geodata
ARG WORLD_SQL_URL=https://raw.githubusercontent.com/dr5hn/countries-states-cities-database/3f1f31dda8d758704b0ed900aaeb64268514cfce/psql/world.sql
ARG GEOIP_YM=2026-10
RUN apt-get update \
 && apt-get install -y --no-install-recommends ca-certificates curl \
 && rm -rf /var/lib/apt/lists/*
# The dump was made by psql >= 16.10; older clients do not know \restrict.
RUN mkdir -p /data/geoip \
 && curl -fsSL "$WORLD_SQL_URL" -o /data/world.sql \
 && sed -i -e '/^\\restrict/d' -e '/^\\unrestrict/d' /data/world.sql \
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

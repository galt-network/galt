#!/usr/bin/env bash
set -euo pipefail
# DB-IP IP to City Lite (CC-BY 4.0). Do not commit the MMDB.
# https://db-ip.com/db/lite.php
ym="${GEOIP_YM:-$(date +%Y-%m)}"
dest="${GALT_GEOIP_MMDB:-data/geoip/dbip-city-lite.mmdb}"
url="https://download.db-ip.com/free/dbip-city-lite-${ym}.mmdb.gz"
mkdir -p "$(dirname "$dest")"
tmp="$(mktemp)"
trap 'rm -f "$tmp"' EXIT
curl -fsSL "$url" -o "$tmp"
gunzip -c "$tmp" > "$dest"
echo "Wrote $dest"

#!/usr/bin/env bash
# Entry point for the chrome-devtools MCP server.
#
# Inside the clj-contain container the browser lives on the host: the key mount
# (/run/cdp-key, see the :clj-contain alias in deps.edn) and podman's
# host.containers.internal are available, so bridge over SSH.
#
# Anywhere else (agent sandbox, plain dev checkout) run chrome-devtools-mcp
# locally against the installed Chrome. CHROME_BIN overrides the browser path,
# CHROME_HEADED=1 disables headless mode.
set -euo pipefail

KEY=/run/cdp-key/id_ed25519
if [[ -r "$KEY" ]] && getent hosts host.containers.internal >/dev/null 2>&1; then
  exec ssh -T -o BatchMode=yes -o StrictHostKeyChecking=yes -o CheckHostIP=no \
    -o UpdateHostKeys=no -o LogLevel=ERROR -o IdentitiesOnly=yes \
    -o UserKnownHostsFile=/run/cdp-key/known_hosts -i "$KEY" \
    madis@host.containers.internal
fi

args=(--no-usage-statistics --allow-unrestricted-paths --chrome-arg=--no-sandbox)
[[ "${CHROME_HEADED:-0}" == "1" ]] || args+=(--headless)

CHROME="${CHROME_BIN:-/opt/chrome-for-testing/chrome-linux64/chrome}"
[[ -x "$CHROME" ]] && args+=(--executablePath "$CHROME")

# chrome-devtools-mcp defaults to one shared profile dir, so two OpenCode
# connections for the same project (or a leftover server) fight over the same
# Chrome instance and the connection dies with "Target closed". Use a temporary
# profile per server process; log in again after a reconnect if a session was
# needed. Set CHROME_USER_DATA_DIR to keep a persistent profile instead.
if [[ -n "${CHROME_USER_DATA_DIR:-}" ]]; then
  args+=(--userDataDir "$CHROME_USER_DATA_DIR")
else
  args+=(--isolated)
fi

exec npx -y chrome-devtools-mcp@latest "${args[@]}"

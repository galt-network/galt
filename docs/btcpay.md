# BTCPay Server (local regtest)

Galt does not talk to BTCPay yet. This stack is the dev environment for that work: a regtest BTCPay plus a second Core Lightning node that can pay its invoices. clj-contain does not know about it.

`scripts/btcpay.bb` owns the stack. Both flows below call that script. The only difference is where the Galt process runs.

## Run

One-time on the host machine, if it is missing:

```
127.0.0.1 btcpay.galt.test
```

The script checks for that line and does not write `/etc/hosts`. Do not use `*.localhost` or `127.0.0.1` as `BTCPAY_HOST`. Chrome drops cookies on a raw IP, and `*.localhost` resolves to `::1` on this machine while the port is published on IPv4.

Stop Polar first. The merchant node publishes host port `9735`.

```
bb scripts/btcpay.bb up
bb scripts/btcpay.bb fund
```

UI: http://btcpay.galt.test:49392

`up` is idempotent. `down` stops containers and leaves volumes. `reset` deletes volumes (chain, channel, BTCPay database, payer). `clj-contain down` does not stop this stack.

`status`, `logs [container]`, `pay <bolt11>`.

First account is a UI step. There is no admin key until a user exists. Open `/register` (not `/account/register`), create the account, create a store, create an API key. Put the key in `.env` as `BTCPAY_API_KEY`. Put the store id in `.env` as `BTCPAY_STORE_ID`. Do not commit either. Do not put URLs in `.env`.

Permissions the next change needs, selected in that UI: `btcpay.store.cancreateinvoice`, `btcpay.store.canviewinvoices`, and webhook modify if Galt will register the callback itself.

`fund` mines 101 blocks if the chain is shorter, sends regtest coins to both Lightning nodes, and opens a channel from the payer to the merchant if one is not already `CHANNELD_NORMAL`. Wallet name inside bitcoind is `galt` (created if missing). `pay` runs `lightning-cli pay` in `galt_payer_clightning`.

Verified on this pin: `fund` prints `channel active`, and `pay` of a merchant `lightning-cli invoice` returns `"status": "complete"`. Invoice creation may warn about incoming capacity; the direct channel still pays.

## Two ways to run Galt

Start the stack first in both cases. BTCPay's Postgres (`generated_postgres_1`) is not Galt's database. Do not point Galt at it.

### Galt inside clj-contain

`deps.edn` `:clj-contain` already sets:

- container name `galt-dev`
- extra network `galt-btcpay_default` (must exist; `up` creates it)
- `BTCPAY_URL=http://btcpayserver:49392`
- `GALT_WEBHOOK_URL=http://galt-dev:8081/payments/btcpay-webhook`

```
bb scripts/btcpay.bb up
clj-contain up
```

The app container name resolves from the BTCPay container on that network (`getent hosts galt-dev`). No clj-contain change.

Browser checkout still uses http://btcpay.galt.test:49392. The in-container URL is only for the Galt process.

### Galt on the host

Same `up`. Then the usual REPL (`GALT_ENV=dev` so `start-system!` loads `config/dev-galt.edn`).

That file has:

- `:btcpay-url "http://btcpay.galt.test:49392"`
- `:galt-webhook-url "http://host.containers.internal:8081/payments/btcpay-webhook"`

`host.containers.internal` from the BTCPay container is `169.254.1.2`. The host app does not load `.env`. Source it yourself (`set -a && source .env && set +a`) for the API key only.

clj-contain merge order is alias < `.clj-contain/config.edn` < `.env` < CLI. A host URL in `.env` would override `BTCPAY_URL` inside the app container. Secrets in `.env`, URLs in the alias or in `dev-galt.edn`.

The two webhook URLs cannot both be stored on the store. This session stored the `galt-dev` one. Boot registration is not built.

## Wired

Galt talks to BTCPay, not Core Lightning.

- `env-overrides` reads `BTCPAY_URL`, `BTCPAY_API_KEY`, `BTCPAY_STORE_ID`, `BTCPAY_WEBHOOK_SECRET`. Process env wins, then `.env`, then EDN.
- `:gateways :payment` is `btcpay_payment_gateway`. Lightning only (`BTC-LN`). The page still renders `:bolt-11`.
- `POST /payments/btcpay-webhook` checks `BTCPay-Sig` on the raw body, then `InvoiceSettled` runs the existing update-invoice use case and pushes the SSE signal. No connection means no push.
- The store webhook points at `http://galt-dev:8081/payments/btcpay-webhook`, events `InvoiceSettled` only. Galt does not register it at boot. The limited key cannot.
- `:cln-rest-url` and `:cln-rune` are gone. `:expiry` seconds become `checkout.expirationMinutes`, rounded up.

Host and container both need `GALT_ENV=dev`. The host webhook URL in `dev-galt.edn` is not the one stored on the store.

## Invoice shape

Protocol: `galt.payments.domain.payment-gateway/PaymentGateway`. `create-invoice` takes `{:amount-msat :label :description :expiry}`. Membership asks for 960 sats (`(* 960 1000)` msat) and keeps `:bolt-11`, `:expires-at`, `:payment-hash`. It still selects `:payment-secret` and `:created-index`; the adapter omits them.

Greenfield, from the Galt process:

```
POST /api/v1/stores/{storeId}/invoices
Authorization: token <BTCPAY_API_KEY>
Content-Type: application/json

{"amount":"960","currency":"SATS","metadata":{"orderId":"<label>"}}
```

`orderId` is the Galt invoice label, so the webhook can find the row. Amount is sats, not msat.

The create response has `id` and `checkoutLink`. The BOLT11 is on the payment method, not always on the create body:

```
GET /api/v1/stores/{storeId}/invoices/{invoiceId}/payment-methods
```

Use the `BTC-LN` entry. Put that destination in `:bolt-11`. Interactive API reference: https://docs.btcpayserver.org/API/Greenfield/ and `/docs` on the instance if you want to confirm a field against this version (2.4.5).

Webhooks are store-scoped. BTCPay POSTs JSON and sets `BTCPay-Sig: sha256=<hex>`. The hex is HMAC-SHA256 of the **raw** body with the webhook secret. Check the signature before parsing. `InvoiceSettled` means paid; then call the existing update-invoice use case with the label from `metadata.orderId`. Do not reuse the CLN webhook parser. That one expects lightningd-webhook's `invoice_payment` map.

SSRF protection is off in this dev stack (`BTCPAY_DISABLESSRFPROTECTION=true` on the BTCPay container only), otherwise callbacks to `galt-dev` and `host.containers.internal` are dropped. Do not copy that flag to a public server.

## What the script actually starts

Pinned `btcpayserver-docker` commit, `sha` in `scripts/btcpay.bb`:

```
cc57300cfe516524588ac3771ab4d51fed1be369
```

To bump: change `sha`, run `up`. It fetches that commit into gitignored `.btcpay/src` and regenerates compose. Read the generated file once after a bump. The script rewrites two things podman rejects: short image names (this host has no `registries.conf`, so they must be `docker.io/...`) and `links:` blocks (`link is not supported`). Service DNS on `galt-btcpay_default` replaces links. Do not run upstream `build.sh` or `btcpay-setup.sh`. `build.sh` calls Docker, so the stack would land on the wrong engine and clj-contain could not join the network. `btcpay-setup.sh -i` wants root, systemd, and a public domain.

Generator flags: `btc`, `clightning`, reverse proxy `none`, exclude `opt-add-tor` and `btcpay-host` (both want the Docker socket or SSH). Network `regtest`, `BTCPAY_PROTOCOL=http` (registration returns 400 on https-without-tls), published port `49392`. A second compose file `.btcpay/dev.override.yml` sets `BTCPAY_DISABLE_REGISTRATION=false` and `BTCPAY_DISABLESSRFPROTECTION=true`. Registration is on: `/` redirects to `/register`, and that page is the create-account form.

Images at this pin: `btcpayserver/btcpayserver:2.4.5`, `btcpayserver/bitcoin:31.1-1`, `btcpayserver/lightning:v26.06.9`, `btcpayserver/postgres:18.6`, `nicolasdorier/nbxplorer:2.6.19`. The payer container `galt_payer_clightning` is not part of upstream compose. It uses the same Lightning image, the bitcoin datadir volume (cookie auth), and its own lightning volume. `lightning-cli` socket on both nodes: `/root/.lightning/lightning-rpc`.

Upstream fragments set fixed `container_name`s (`generated_btcpayserver_1`, `btcpayserver_bitcoind`, `btcpayserver_clightning_bitcoin`). One BTCPay stack per machine.

Not in this setup: nginx, Let's Encrypt, Tor, cheat mode, calling CLN REST (port 3010) through BTCPay. BTCPay talks to the merchant node over the unix socket. Galt should call Greenfield only.

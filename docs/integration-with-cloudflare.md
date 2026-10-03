# Cloudflare Integration — R2 Setup + Verification

How to wire GALT's dev environment to Cloudflare R2 so uploaded files (avatars, event
images, post photos, videos, 3D models, zips) land in an R2 bucket and are publicly served
from `https://media.galt.is/<key>`.

## Prerequisites

- The `galt.is` zone is active on Cloudflare (proxied) — the custom domain step requires it.
- `.env` is gitignored; put credentials there or export them in your shell. Never in git
  (note: `config/dev-galt.edn` already contains a real CLN rune — never print the file).

## 1. Cloudflare dashboard steps (one-time)

### 1.1 Create the R2 bucket

1. Go to dash.cloudflare.com → **R2** → **Create bucket**.
2. Name it `galt-media` (name is config, any works). Region does not matter for R2.

### 1.2 Connect custom domain `media.galt.is`

1. In the bucket, go to **Settings → Public access**.
2. Click **Connect custom domain**, enter `media.galt.is`.
3. Cloudflare auto-creates the DNS CNAME (`media` → bucket). It appears in **DNS →
   Records** for the `galt.is` zone; make sure it is proxied (orange cloud).
4. This is **required**: the app's `:public-base-url` is `https://media.galt.is`. Public
   files render as `https://media.galt.is/<key>` — do not rely on the R2.dev subdomain
   instead, or rendered URLs will break.

### 1.3 CORS rule (precautionary)

1. Same bucket **Settings** page → **CORS** → add rule:

   - Allowed origins: `https://galt.is`, `https://media.galt.is`, `http://localhost:8081`
   - Methods: GET, HEAD
   - Allowed headers: `*`

2. Only matters for direct browser fetches from the media domain — server-side
   uploads/redirects don't need it, but it future-proofs `<img>`/canvas usage.

### 1.4 API token

1. **R2 → Manage API tokens → Create API token**.
2. Permissions: **Object Read & Write** on the `galt-media` bucket.
3. Copy the **Access Key ID** and **Secret Access Key**.
4. Note your **Account ID** (shown on the R2 dashboard, also in the dashboard URL) — needed
   for the endpoint in step 2.

## 2. Local config fill-in

Edit `config/dev-galt.edn` → `:asset-store`:

```clojure
:asset-store {:type :s3                          ; was :disk (commented out)
              :storage-root "resources/uploads"  ; disk only, leave as-is
              :root-url "https://dev.galt.is"    ; app origin (private proxy base)
              :bucket "galt-media"
              :endpoint "https://<ACCOUNT_ID>.r2.cloudflarestorage.com"  ; real account id
              :region "auto"
              :public-base-url "https://media.galt.is"
              :public-prefix "public"
              :private-prefix "private"
              :access-key nil                    ; nil → env R2_ACCESS_KEY_ID
              :secret-key nil                    ; nil → env R2_SECRET_ACCESS_KEY
              :upload-config {:image-max-bytes 26214400
                              :video-max-bytes 104857600
                              :model-max-bytes 104857600
                              :archive-max-bytes 104857600}}
```

Export credentials where the dev server runs (or add to `.env`):

```bash
export R2_ACCESS_KEY_ID=...
export R2_SECRET_ACCESS_KEY=...
```

## 3. Migrations

```bash
clojure -M:dev:repl -m galt.main migrate
```

Applies `create-assets` + the avatar URL→key rewrite (idempotent, safe to re-run).

## 4. Restart and test end-to-end

Restart the server (kill JVM, restart nREPL, `(galt.main/start-system! :dev)`).

### 4.1 Public upload (avatar)

1. Log in, upload a profile avatar.
2. DB check:

   ```clojure
   ;; in REPL
   (require '[galt.core.adapters.db-asset-repository :as r])
   (r/list-objects ...) ; or query the assets table directly
   ```

   Expect: row with `sha256`, `storage_key = public/<sha256>.png`, `visibility = public`.
3. URL check:

   ```bash
   curl -sI https://media.galt.is/public/<sha256>.png
   ```

   Expect `200`, `Content-Type: image/png`, `Cache-Control: public, max-age=31536000, immutable`.
4. Open the same URL in a browser — the avatar must render on the profile page.

### 4.2 Dedup

Re-upload the same file. Expect: same `storage_key`, no new object stored (re-upload of
same content + visibility reuses the existing asset row).

### 4.3 Private assets

1. Upload a private asset (e.g. a post/event file marked private), then hit
   `/private-assets/<key>`:
   - anonymous → 401 / redirect to login
   - logged in → streams with `Content-Type`/`Content-Length`, `Cache-Control: private, max-age=60`
2. The private object must NOT be reachable at `https://media.galt.is/private/<key>` (404).

### 4.4 CF Images resize (optional, non-blocking)

```bash
curl -sI 'https://media.galt.is/public/<key>.png?width=400&format=auto'
```

Expect a 200 with a resized image. Free tier ≈ 5,000 unique transformations/month. Fallback
if it fights us: on-request thumbnails later.

### 4.5 Backup sanity check

```bash
clojure -M:dev:repl -m galt.main assets export-public /tmp/galt-media-export
```

Expect the public keys mirrored under `/tmp/galt-media-export`.

## 5. Troubleshooting

| Symptom | Cause / fix |
|---|---|
| App boots fine with `:type :s3` + no creds, but upload fails with "S3/R2 credentials missing" | Client is lazy; creds are checked on first use. Export `R2_ACCESS_KEY_ID` / `R2_SECRET_ACCESS_KEY`. |
| `s3 PutObject failed: AccessDenied` | Token scope wrong (must be Object Read & Write on the bucket), or wrong account id in `:endpoint`. |
| `s3 PutObject failed: ... "No known endpoint."` | aws-api can't resolve R2's region-less `"auto"` against its AWS endpoint table. The asset store maps `"auto"` → `us-east-1` internally (R2 ignores it; the endpoint override fixes the host). If you still see this, the server is running stale code — reload/restart. |
| `NoSuchBucket` | `:bucket` name doesn't match the created bucket. |
| Rendered URLs point at `localhost` | `:public-base-url` not set to `https://media.galt.is`. |
| `https://media.galt.is/...` 404s | Custom domain not connected (step 1.2) or DNS record not proxied. |
| Local dev without a bucket | Flip `:asset-store :type` back to `:disk` — disk store is the dev/test default and needs no R2. |

## Notes

- Uploads from a localhost dev server reach R2 over the internet — no tunnel needed.
- Same steps apply to staging/prod: fill `:bucket`/`:endpoint` in `config/staging-galt.edn`
  / `config/prod-galt.edn`, export creds on the server, re-run migrations.
- Full ops reference (rclone backups, fork setup, limits): `docs/assets.md`.

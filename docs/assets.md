# Media Assets (R2 + Cloudflare CDN)

Unified asset pipeline for all user media: event images, post photos, avatars, short videos,
GLB/glTF 3D objects, downloadable zips. One mechanism serves the whole app and a future WYSIWYG
post editor.

## Architecture

```
Upload → POST /files (Datastar SSE or plain) → upload use case → AssetStore → R2 bucket
Delivery (public)  → https://media.galt.is/<key>            (R2 custom domain / CDN)
Delivery (private) → https://galt.is/private-assets/<key>    (app proxy, behind auth)
Backups           → rclone R2 → B2 / local
```

- **Storage**: Cloudflare R2 (S3-compatible). Implementation is provider-agnostic
  (`galt.core.domain.asset-store/AssetStore` protocol) — B2/Hetzner/MinIO are config swaps.
- **Delivery**: custom domain `media.galt.is` on the Cloudflare CDN pointing at the public R2
  bucket. App JS/CSS stays on the app origin; user media is the only thing on the bucket.
- **Keys**: relative keys stored in DB (`public/<sha256>.png`, `private/<sha256>.mp4`).
  Absolute URLs are composed at render time via the `:asset-url` helper — changing domains
  never breaks rows.
- **Dedup**: same content + same visibility → same key; re-uploads skip storage and reuse the
  existing asset row.

## R2 bucket setup (one-time, Cloudflare dashboard)

1. Create bucket `galt-media` (name is config, any works).
2. R2 → Settings → Public access: enable the public bucket (R2.dev subdomain) or better,
   add a **custom domain** `media.galt.is` (requires `media` DNS CNAME to the R2 bucket;
   Cloudflare manages this when the zone is proxied).
3. Settings → CORS: allow origins `https://galt.is` and `https://media.galt.is` (and dev
   origins) with methods GET, HEAD, and headers `*`. Used if anything is ever fetched from
   the browser with CORS.
4. R2 → Manage API tokens: create an API token with object read/write on the bucket.
5. Set env vars on the server (never in git):

   ```bash
   export R2_ACCESS_KEY_ID=...
   export R2_SECRET_ACCESS_KEY=...
   ```

6. Fill `:bucket` / `:endpoint` in the config's `:asset-store` map
   (`https://<ACCOUNT_ID>.r2.cloudflarestorage.com`).

## Configuration

`config/<env>-galt.edn`, key `:asset-store`:

```clojure
:asset-store {:type :disk | :s3
              :storage-root "resources/uploads"     ; disk only
              :root-url "https://galt.is"           ; app origin (private proxy base)
              :bucket "galt-media"                  ; s3 only
              :endpoint "https://<ACCOUNT_ID>.r2.cloudflarestorage.com"
              :region "auto"                        ; R2 is region-less
              :public-base-url "https://media.galt.is"
              :public-prefix "public"
              :private-prefix "private"
              :access-key nil                       ; nil → env R2_ACCESS_KEY_ID
              :secret-key nil                       ; nil → env R2_SECRET_ACCESS_KEY
              :upload-config {:image-max-bytes 26214400      ; 25 MB
                              :video-max-bytes 104857600     ; 100 MB
                              :model-max-bytes 104857600     ; 100 MB (glb/gltf)
                              :archive-max-bytes 104857600}} ; 100 MB (zip)
```

- `:type :disk` = local filesystem (`<storage-root>/<key>`), the dev/test default.
- `:type :s3` = R2 via the S3 API. Default dev config is `:s3` — flip to `:disk` for local
  development without an R2 bucket/creds.
- Secrets: `:access-key`/`:secret-key` in config are optional; env vars are preferred.

## Upload limits

| Category   | Types                                              | Max size |
|------------|----------------------------------------------------|----------|
| image      | jpg, png, webp, avif, gif                          | 25 MB    |
| video      | mp4, webm                                          | 100 MB   |
| model      | glb, gltf                                          | 100 MB   |
| archive    | zip                                                | 100 MB   |

Exceeding limits or disallowed types → SSE error notification (`:is-danger`) or HTTP 400
`{:errors [...]}` on the plain path.

## Image resizing (Cloudflare Images)

Free tier includes ~5,000 unique transformations/month for images stored in R2. Resizing is
URL-param based, no stored duplicates:

```
https://media.galt.is/<key>?width=400&format=auto
```

The `:asset-url` helper accepts an opts map appended as a query string:
`((:asset-url fs) key {:width 400 :format "auto"})`. Verify variant wiring in the CF
dashboard; on-request thumbnails remain a documented fallback if the free tier fights us.

## Private assets

Keys under `private/` are never on the public bucket. Served through the app at
`/private-assets/<key>` behind `wrap-auth` (`:min-role :user`), streamed from R2 with
correct content-type/length and `Cache-Control: private, max-age=60`. Presigned URLs are
available via the protocol (`presign-url`) for later use.

## Backups (rclone)

```bash
rclone config    # remote "r2": type s3, provider Cloudflare, R2 access/secret keys,
                 # endpoint https://<ACCOUNT_ID>.r2.cloudflarestorage.com
rclone sync r2:galt-media b2:galt-backups/galt-media   # R2 → B2
rclone sync r2:galt-media /backup/galt-media           # R2 → local
```

CLI export (downloads all public objects, mirrors keys under the out dir):

```bash
clojure -M:dev:repl -m galt.main assets export-public <out-dir>
java -jar galt.jar assets export-public <out-dir>
```

## Fork setup

1. Create an R2 bucket (or any S3-compatible bucket), set CORS + custom domain.
2. Set `R2_ACCESS_KEY_ID` / `R2_SECRET_ACCESS_KEY` env vars.
3. Point `:asset-store` config at the bucket; set `:public-base-url` to your media domain.
4. `clojure -M:dev:repl -m galt.main migrate`
5. Optionally seed from an existing deployment:
   `rclone sync r2:their-bucket r2:your-bucket`

## Database

`assets` table: `id uuid pk`, `sha256`, `content_type`, `size`, `original_filename`,
`storage_key unique`, `visibility ('public'|'private')`, `owner_member_id` (nullable FK,
set null on member delete), `module` (avatar/post/event/...), `created_at`. Posts and
events carry a JSONB `asset_ids` column for WYSIWYG media attachments.

Legacy avatars stored as absolute `/files/<uuid>__<name>` URLs are rewritten to bare keys
by migration `20260810120010-rewrite-avatar-urls-to-keys`.

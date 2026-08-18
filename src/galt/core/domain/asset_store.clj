(ns galt.core.domain.asset-store
  "Protocol for object storage backends (disk, S3-compatible like R2/B2/MinIO).
  Data in/out only - no web-framework specifics. Keys are relative storage keys
  (e.g. \"public/<sha256>.png\"), never absolute URLs."
  (:import
    [java.io InputStream]))

(defprotocol AssetStore
  (put-object
    [this asset]
    "Store an asset. asset: {:key string, :content-type string, :size long,
     :input-stream InputStream, :filename string}. Implementations may not
     buffer the whole file in memory.")
  (get-object
    [this key]
    "Return an InputStream of the object contents (or nil if missing).")
  (get-content-response
    [this key]
    "Return a Ring response map for serving the object.")
  (delete-object
    [this key]
    "Delete the object. No-op if it does not exist.")
  (presign-url
    [this key expires-in-seconds]
    "Return a time-limited signed URL for the object.")
  (list-objects
    [this prefix]
    "Return a seq of keys under the given prefix (nil or \"\" = all)."))

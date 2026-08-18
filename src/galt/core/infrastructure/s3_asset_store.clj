(ns galt.core.infrastructure.s3-asset-store
  "S3-compatible (Cloudflare R2, B2, MinIO, Hetzner) implementation of the
  AssetStore protocol. Provider-agnostic: all provider specifics come from
  the config map (:endpoint, :bucket, :region, credentials)."
  (:require
   [clojure.string :as str]
   [cognitect.aws.credentials :as creds]
   [cognitect.aws.client.api :as aws]
   [galt.core.domain.asset-store :refer [AssetStore]]
   [galt.core.infrastructure.s3-presign :as s3-presign]
   [lambdaisland.uri :as uri]))

(defn public-key?
  "Keys under the private prefix are private; everything else (public prefix,
  legacy uuid__name keys) is public."
  [config key]
  (not (str/starts-with? key (str (:private-prefix config) "/"))))

(defn- throw-on-anomaly [result op]
  (when (:cognitect.anomalies/category result)
    (throw (ex-info (str "s3 " op " failed: "
                         (:cognitect.aws.error/code result)
                         " " (:cognitect.aws.error/message result))
                    {:op op :anomaly result})))
  result)

(defn- aws-client-region
  "Returns the region passed to the aws-api client. R2 is region-less and its
  config convention is \"auto\", but aws-api resolves the region against its
  bundled endpoint table (real AWS regions only) BEFORE applying the endpoint
  override, so \"auto\" fails with `No known endpoint.`. Map it to a
  resolvable region; :endpoint-override fixes the real host and R2 ignores the
  region in the SigV4 credential scope."
  [{:keys [region]}]
  (let [region (or region "auto")]
    (if (= region "auto") "us-east-1" region)))

(defn- build-client
  "Builds the aws-api client. Credentials fall back to the
  R2_ACCESS_KEY_ID / R2_SECRET_ACCESS_KEY env vars. The client is built
  lazily so the app boots even without credentials configured; first use
  fails with a descriptive error."
  [{:keys [endpoint bucket access-key secret-key] :as config}]
  (let [access-key (or access-key (System/getenv "R2_ACCESS_KEY_ID"))
        secret-key (or secret-key (System/getenv "R2_SECRET_ACCESS_KEY"))]
    (when-not (and access-key secret-key)
      (throw (ex-info (str "S3/R2 credentials missing. Set :access-key and "
                           ":secret-key in config (or the R2_ACCESS_KEY_ID / "
                           "R2_SECRET_ACCESS_KEY env vars) before using the "
                           "S3 asset store.")
                      {:endpoint endpoint :bucket bucket})))
    (let [endpoint-uri (uri/uri endpoint)]
      (aws/client
       {:api :s3
        :region (aws-client-region config)
        :endpoint-override {:protocol (keyword (:scheme endpoint-uri))
                            :hostname (:host endpoint-uri)}
        :credentials-provider
        (creds/basic-credentials-provider
         {:access-key-id access-key
          :secret-access-key secret-key})}))))

(defrecord S3AssetStore [client config]
  AssetStore

  (put-object [_ {:keys [key content-type size input-stream]}]
    (let [cache-control (if (public-key? config key)
                          "public, max-age=31536000, immutable"
                          "private, max-age=60")
          result (aws/invoke @client
                             {:op :PutObject
                              :request {:Bucket (:bucket config)
                                        :Key key
                                        :Body input-stream
                                        :ContentType content-type
                                        :ContentLength (long size)
                                        :CacheControl cache-control}})]
      (throw-on-anomaly result "PutObject")
      {:key key :content-type content-type :size size}))

  (get-object [_ key]
    (-> (throw-on-anomaly
         (aws/invoke @client {:op :GetObject
                              :request {:Bucket (:bucket config) :Key key}})
         "GetObject")
        :Body))

  (get-content-response [_ key]
    (if (public-key? config key)
      {:status 302
       :headers {"Location" (str (:public-base-url config) "/" key)}}
      (let [result (throw-on-anomaly
                    (aws/invoke @client {:op :GetObject
                                         :request {:Bucket (:bucket config) :Key key}})
                    "GetObject")]
        {:status 200
         :headers {"Content-Type" (or (:ContentType result) "application/octet-stream")
                   "Content-Length" (str (:ContentLength result))
                   "Cache-Control" "private, max-age=60"}
         :body (:Body result)})))

  (delete-object [_ key]
    (throw-on-anomaly
     (aws/invoke @client {:op :DeleteObject
                          :request {:Bucket (:bucket config) :Key key}})
     "DeleteObject"))

  (presign-url [_ key expires-in-seconds]
    (let [{:keys [endpoint bucket region access-key secret-key]} config]
      (s3-presign/presign-url
       {:host (:host (uri/uri endpoint))
        :uri (str "/" bucket "/" key)
        :region (or region "auto")
        :access-key (or access-key (System/getenv "R2_ACCESS_KEY_ID"))
        :secret-key (or secret-key (System/getenv "R2_SECRET_ACCESS_KEY"))
        :expires-in-seconds expires-in-seconds
        :scheme (or (:scheme (uri/uri endpoint)) "https")})))

  (list-objects [_ prefix]
    (loop [token nil keys []]
      (let [result (throw-on-anomaly
                    (aws/invoke @client
                                {:op :ListObjectsV2
                                 :request (cond-> {:Bucket (:bucket config) :Prefix prefix}
                                            token (assoc :ContinuationToken token))})
                    "ListObjectsV2")
            keys' (into keys (map :Key (:Contents result)))]
        (if-let [next-token (:NextContinuationToken result)]
          (recur next-token keys')
          keys')))))

(defn new-s3-asset-store
  "Builds an S3-compatible (R2/B2/MinIO/Hetzner) asset store.

  Config keys: :endpoint (https://<account>.r2.cloudflarestorage.com),
  :bucket, :access-key/:secret-key (fall back to R2_ACCESS_KEY_ID /
  R2_SECRET_ACCESS_KEY env vars), :region (R2 is region-less, \"auto\")."
  [config]
  (map->S3AssetStore {:client (delay (build-client config)) :config config}))

(ns galt.core.infrastructure.s3-presign
  "SigV4 query-string (presigned URL) signing for S3-compatible object stores
  (R2, B2, MinIO). Matches botocore's S3SigV4QueryAuth: auth params are part
  of the signed canonical request, payload is UNSIGNED-PAYLOAD."
  (:require
   [clojure.string :as str])
  (:import
   [javax.crypto Mac]
   [javax.crypto.spec SecretKeySpec]
   [java.nio.charset StandardCharsets]))

(defn- utf8 [^String s]
  (.getBytes s StandardCharsets/UTF_8))

(defn- hmac [^bytes key ^bytes data]
  (let [mac (Mac/getInstance "HmacSHA256")]
    (.init mac (SecretKeySpec. key "HmacSHA256"))
    (.doFinal mac data)))

(defn- hex [^bytes bs]
  (let [sb (StringBuilder.)]
    (doseq [b bs]
      (.append sb (format "%02x" (bit-and b 0xff))))
    (.toString sb)))

(defn- sha256-hex [^bytes data]
  (hex (.digest (java.security.MessageDigest/getInstance "SHA-256") data)))

(defn- url-encode
  "RFC 3986 percent-encoding (S3 presigned URLs)."
  [^String s]
  (let [unreserved (set "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_.~")]
    (apply str
           (map (fn [^Character c]
                  (if (contains? unreserved c)
                    (str c)
                    (format "%%%02X" (int (char c)))))
                s))))

(defn- canonical-query
  "Query string exactly as it appears in the canonical request (values must
  already be percent-encoded)."
  [{:keys [algorithm credential date expires signed-headers]}]
  (->> [[:X-Amz-Algorithm algorithm]
        [:X-Amz-Credential credential]
        [:X-Amz-Date date]
        [:X-Amz-Expires (str expires)]
        [:X-Amz-SignedHeaders signed-headers]]
       (map (fn [[k v]] (str (name k) "=" v)))
       (str/join "&")))

(defn- canonical-request
  "The X-Amz-* auth params ARE part of the signed canonical request (as in
  the S3 query-string auth spec); the payload line is the constant
  UNSIGNED-PAYLOAD (matches botocore's S3SigV4QueryAuth)."
  [{:keys [host uri query]}]
  (str/join "\n"
            ["GET"
             uri
             query
             (str "host:" host)
             ""
             "host"
             "UNSIGNED-PAYLOAD"]))

(defn- signing-key [secret date region service]
  (let [k-date (hmac (utf8 (str "AWS4" secret)) (utf8 date))
        k-region (hmac k-date (utf8 region))
        k-service (hmac k-region (utf8 service))]
    (hmac k-service (utf8 "aws4_request"))))

(defn- utc-now []
  (-> (java.time.ZonedDateTime/now (java.time.ZoneId/of "UTC"))
      (.format (java.time.format.DateTimeFormatter/ofPattern "yyyyMMdd'T'HHmmss'Z'"))))

(defn presign-url
  "Build a SigV4-presigned GET URL.

  opts: {:host \"<endpoint-host>\" (no scheme)
         :uri \"/<bucket>/<key>\" (already percent-encoded path)
         :region \"auto\" :access-key \"...\" :secret-key \"...\"
         :expires-in-seconds 3600 :scheme \"https\"
         :timestamp \"20130524T000000Z\" (optional, for testing)}"
  [{:keys [host uri region access-key secret-key expires-in-seconds
           scheme timestamp]
    :or {scheme "https"}}]
  (let [timestamp (or timestamp (utc-now))
        date (subs timestamp 0 8)
        service "s3"
        credential (str/join "/" [access-key date region service "aws4_request"])
        credential-encoded (url-encode credential)
        signed-headers "host"
        query (canonical-query {:algorithm "AWS4-HMAC-SHA256"
                                :credential credential-encoded
                                :date timestamp
                                :expires expires-in-seconds
                                :signed-headers signed-headers})
        canonical (canonical-request {:host host :uri uri :query query})
        scope (str/join "/" [date region service "aws4_request"])
        string-to-sign (str/join "\n"
                                 ["AWS4-HMAC-SHA256"
                                  timestamp
                                  scope
                                  (sha256-hex (utf8 canonical))])
        signature (-> (signing-key secret-key date region service)
                      (hmac (utf8 string-to-sign))
                      hex)]
    (str scheme "://" host uri "?" query "&X-Amz-Signature=" signature)))

(ns galt.core.infrastructure.bitcoin.lnurl
  (:require
    [galt.core.infrastructure.bitcoin.encoding :as encoding]
    [ring.util.codec :as codec]
    [clojure.string :as str])
  (:import
    [java.security SecureRandom]
    [java.net URI]))

(defn- generate-k1 []
  (let [bytes (byte-array 32)]
    (.nextBytes (SecureRandom.) bytes)
    bytes))

(defn add-query-params
  "Add query parameters to a URL. Params is a map of key-value pairs."
  [url params]
  (let [uri (URI. url)
        ;; Get existing query string
        existing-query (.getQuery uri)
        ;; Parse existing query params into a map
        existing-params (if existing-query
                          (->> (str/split existing-query #"&")
                               (map #(str/split % #"="))
                               (into {} (map (fn [[k v]] [(codec/form-decode k) (codec/form-decode v)]))))
                          {})
        ;; Merge new params with existing ones
        merged-params (merge existing-params params)
        ;; Encode merged params into query string
        query-string (->> merged-params
                          (map (fn [[k v]] (str (codec/form-encode (name k))
                                               "="
                                               (codec/form-encode (str v)))))
                          (str/join "&"))]
    ;; Reconstruct URL
    (str (URI. (.getScheme uri)
               (.getUserInfo uri)
               (.getHost uri)
               (.getPort uri)
               (.getPath uri)
               query-string
               (.getFragment uri)))))

(defn generate-lnurl
  [base-url & [query-params]]
  (println ">> generating lnurl" base-url)
  (let [k1 (generate-k1)
        k1-hex (encoding/hex-encode k1)
        callback-url (add-query-params base-url (merge {:tag "login" :k1 k1-hex} query-params))
        lnurl (encoding/encode-bech32 "lnurl" callback-url)]
    {:lnurl lnurl :url callback-url :k1-hex k1-hex}))

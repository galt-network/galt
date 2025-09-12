(ns galt.core.adapters.url-helpers
  (:require
    [ring.util.codec :as codec]
    [clojure.string :as str])
  (:import
    [java.net URI]))

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

(defn decode-url-encoded
  "Decodes a URI parameter string. Use rounds > 1 for double-encoded strings."
  [s]
  (when s (java.net.URLDecoder/decode s "UTF-8")))

(ns galt.core.adapters.url-helpers
  (:require
    [ring.util.codec :as codec]
    [lambdaisland.uri :as uri]))

(defn add-query-params
  "Add query parameters to a URL. Params is a map of key-value pairs."
  [url params]
  (str (uri/assoc-query (uri/uri url) params)))

(defn decode-url-encoded
  "Decodes a URI parameter string"
  [s]
  (when s (java.net.URLDecoder/decode s "UTF-8")))

(comment
  (add-query-params "/hello?wut=is" {:return-to "/home"})
  (codec/form-decode "type=galt-membership-payment&return-to=%252Fmembers%252Fme")
  (codec/form-decode (codec/form-encode "/hello/world")))

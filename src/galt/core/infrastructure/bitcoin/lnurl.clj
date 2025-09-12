(ns galt.core.infrastructure.bitcoin.lnurl
  (:require
    [galt.core.infrastructure.bitcoin.encoding :as encoding]
    [galt.core.adapters.url-helpers :refer [add-query-params]])
  (:import
    [java.security SecureRandom]))

(defn- generate-k1 []
  (let [bytes (byte-array 32)]
    (.nextBytes (SecureRandom.) bytes)
    bytes))

(defn generate-lnurl
  [base-url & [query-params]]
  (let [k1 (generate-k1)
        k1-hex (encoding/hex-encode k1)
        callback-url (add-query-params base-url (merge {:tag "login" :k1 k1-hex} query-params))
        lnurl (encoding/encode-bech32 "lnurl" callback-url)]
    {:lnurl lnurl :url callback-url :k1-hex k1-hex}))

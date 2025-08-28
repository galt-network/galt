(ns galt.core.infrastructure.bitcoin.encoding
  (:require
    [buddy.core.codecs :as codecs])
  (:import [org.bitcoinj.base Bech32 Bech32$Encoding]
           [java.nio.charset StandardCharsets]
           [java.util HexFormat]))

(defn encode-bech32
  "Encodes a string (e.g., URL) into a Bech32 string using the given HRP.
  Input data is converted to UTF-8 bytes internally."
  [hrp data-str]
  (let [data-bytes (.getBytes data-str StandardCharsets/UTF_8)
        encoding Bech32$Encoding/BECH32]  ;; Or BECH32M if needed
    (Bech32/encodeBytes encoding hrp data-bytes)))

(defn decode-bech32
  "Decodes a Bech32 string back to the original data as a string.
  Validates the expected HRP and encoding."
  [bech-str expected-hrp]
  (let [encoding Bech32$Encoding/BECH32  ;; Or BECH32M if needed
        data-bytes (Bech32/decodeBytes bech-str expected-hrp encoding)]
    (String. data-bytes StandardCharsets/UTF_8)))

(defn hex-encode [bytes] (codecs/bytes->hex bytes))
(defn hex-decode [hex-str] (codecs/hex->bytes hex-str))
(defn hex-decode-alternative [hex-str] (.parseHex (HexFormat/of) hex-str))

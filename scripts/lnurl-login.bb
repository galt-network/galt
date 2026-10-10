#!/usr/bin/env bb
;; lnurl-login.bb - LNURL-auth wallet CLI for logging into GALT during development.
;;
;; This acts like a Bitcoin Lightning wallet's LNURL-auth flow: it decodes an
;; lnurl... login string, derives a domain-specific linking key from a seed,
;; signs the k1 challenge and calls GALT's lnurl-auth callback. It never touches
;; GALT internals - only the HTTP endpoints embedded in the LNURL.
;;
;; Usage:
;;   bb scripts/lnurl-login.bb <user> <lnurl>
;;   bb scripts/lnurl-login.bb --list
;;   bb scripts/lnurl-login.bb --generate-hex-seed
;;   bb scripts/lnurl-login.bb --generate-mnemonic
;;   bb scripts/lnurl-login.bb --config other.edn alice <lnurl>
;;
;; Options:
;;   --config PATH       EDN config file (default: scripts/lnurl-login.edn)
;;   --wordlist PATH     BIP39 wordlist (default: scripts/bip39-english.txt)
;;   --verbose           print the full callback URL
;;
;; See scripts/lnurl-login.edn for example users.

(ns lnurl-login
  (:require
   [babashka.fs :as fs]
   [babashka.http-client :as http]
   [cheshire.core :as json]
   [clojure.edn :as edn]
   [clojure.string :as str])
  (:import
   [java.math BigInteger]
   [java.net URI URLDecoder URLEncoder]
   [java.nio.charset StandardCharsets]
   [java.security MessageDigest SecureRandom]
   [java.text Normalizer Normalizer$Form]
   [javax.crypto Mac]
   [javax.crypto.spec SecretKeySpec]))

;;; ---------------------------------------------------------------------------
;;; byte / hex helpers
;;; ---------------------------------------------------------------------------

(defn bytes->hex [^bytes ba]
  (let [sb (StringBuilder.)]
    (dotimes [i (alength ba)]
      (.append sb (format "%02x" (bit-and (aget ba i) 0xff))))
    (.toString sb)))

(defn hex->bytes [s]
  (let [s (str/replace (str s) #"\s" "")
        s (if (odd? (count s)) (str "0" s) s)]
    (byte-array (map (fn [i]
                       (unchecked-byte (Integer/parseInt (subs s (* 2 i) (+ 2 (* 2 i))) 16)))
                     (range (quot (count s) 2))))))

(defn concat-bytes [& arrs]
  (byte-array (mapcat seq arrs)))

(defn subarray [^bytes ba from to]
  (let [n (- to from) out (byte-array n)]
    (System/arraycopy ba from out 0 n)
    out))

(defn barray [& xs] (byte-array (map unchecked-byte xs)))

(defn utf8 ^bytes [s] (.getBytes ^String s StandardCharsets/UTF_8))

(defn bigint->bytes
  "Unsigned big-endian bytes of x, left-padded/truncated to len."
  [x ^long len]
  (let [x (biginteger x)
        raw (.toByteArray ^BigInteger x)
        raw (if (and (> (alength raw) 1) (zero? (aget raw 0)))
              (subarray raw 1 (alength raw))
              raw)]
    (cond
      (> (alength raw) len) (subarray raw (- (alength raw) len) (alength raw))
      (< (alength raw) len) (concat-bytes (byte-array (- len (alength raw))) raw)
      :else raw)))

(defn bytes->bigint [^bytes ba] (BigInteger. 1 ba))

;;; ---------------------------------------------------------------------------
;;; hashing
;;; ---------------------------------------------------------------------------

(defn sha256 [^bytes ba] (.digest (MessageDigest/getInstance "SHA-256") ba))

(defn hmac [algo ^bytes key ^bytes msg]
  (let [m (Mac/getInstance algo)]
    (.init m (SecretKeySpec. key algo))
    (.doFinal m msg)))

(defn hmac-sha256 [k m] (hmac "HmacSHA256" k m))
(defn hmac-sha512 [k m] (hmac "HmacSHA512" k m))

(defn pbkdf2-sha512
  "PBKDF2-HMAC-SHA512 (babashka has no java.security.spec.PBEKeySpec)."
  [^bytes password ^bytes salt ^long iterations ^long dk-len]
  (let [h-len 64
        blocks (long (Math/ceil (/ (double dk-len) h-len)))
        out (java.io.ByteArrayOutputStream.)]
    (dotimes [i blocks]
      (let [i-bytes (bigint->bytes (BigInteger/valueOf (inc i)) 4)
            u1 (hmac-sha512 password (concat-bytes salt i-bytes))
            t (byte-array u1)]
        (loop [j 1 u u1]
          (when (< j iterations)
            (let [u2 (hmac-sha512 password u)]
              (dotimes [b (alength t)]
                (aset t b (unchecked-byte (bit-xor (aget t b) (aget u2 b)))))
              (recur (inc j) u2))))
        (.write out t 0 (alength t))))
    (subarray (.toByteArray out) 0 dk-len)))

(defn random-bytes [^long n]
  (let [b (byte-array n)]
    (.nextBytes (SecureRandom.) b)
    b))

;;; ---------------------------------------------------------------------------
;;; secp256k1
;;; ---------------------------------------------------------------------------

(def ^BigInteger p (BigInteger. "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F" 16))
(def ^BigInteger n (BigInteger. "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141" 16))
(def ^BigInteger gx (BigInteger. "79BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798" 16))
(def ^BigInteger gy (BigInteger. "483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8" 16))
(def G [gx gy])

(defn mod-p ^BigInteger [x] (.mod (biginteger x) p))
(defn mod-n ^BigInteger [x] (.mod (biginteger x) n))

(defn b+ ^BigInteger [a b] (.add ^BigInteger (biginteger a) ^BigInteger (biginteger b)))
(defn b- ^BigInteger [a b] (.subtract ^BigInteger (biginteger a) ^BigInteger (biginteger b)))
(defn b* ^BigInteger [a b] (.multiply ^BigInteger (biginteger a) ^BigInteger (biginteger b)))

(defn point-double [P]
  (let [[x y] P]
    (if (nil? x)
      nil
      (let [m (mod-p (b* (mod-p (b* x x)) (BigInteger/valueOf 3)))
            m (mod-p (b* m (.modInverse (mod-p (b* y (BigInteger/valueOf 2))) p)))
            x3 (mod-p (b- (b* m m) (b* x (BigInteger/valueOf 2))))
            y3 (mod-p (b- (b* m (b- x x3)) y))]
        [x3 y3]))))

(defn point-add [P Q]
  (cond
    (nil? P) Q
    (nil? Q) P
    (= P Q) (point-double P)
    (= (first P) (first Q)) nil
    :else (let [[x1 y1] P [x2 y2] Q
                m (mod-p (b* (b- y2 y1) (.modInverse (mod-p (b- x2 x1)) p)))
                x3 (mod-p (b- (b- (b* m m) x1) x2))
                y3 (mod-p (b- (b* m (b- x1 x3)) y1))]
            [x3 y3])))

(defn point-mul [k P]
  (loop [k (mod-n k) acc nil addend P]
    (if (zero? (.signum ^BigInteger k))
      acc
      (recur (.shiftRight ^BigInteger k 1)
             (if (.testBit ^BigInteger k 0) (point-add acc addend) acc)
             (point-double addend)))))

(defn pubkey-compressed-bytes [^BigInteger priv]
  (let [[x y] (point-mul priv G)
        prefix (if (.testBit y 0) (byte 0x03) (byte 0x02))]
    (concat-bytes (byte-array [prefix]) (bigint->bytes x 32))))

(defn pubkey-compressed-hex [^BigInteger priv]
  (bytes->hex (pubkey-compressed-bytes priv)))

;; RFC 6979 deterministic nonce (HMAC-SHA256).
(defn rfc6979-k [^bytes x-bytes ^bytes h-bytes]
  (let [h1 (mod-n (bytes->bigint h-bytes))
        h1-bytes (bigint->bytes h1 32)
        v (byte-array 32 (byte 1))
        k (byte-array 32 (byte 0))
        k (hmac-sha256 k (concat-bytes v (barray 0) x-bytes h1-bytes))
        v (hmac-sha256 k v)
        k (hmac-sha256 k (concat-bytes v (barray 1) x-bytes h1-bytes))
        v (hmac-sha256 k v)]
    (loop [k k v v]
      (let [t (loop [v v acc (byte-array 0)]
                (if (>= (alength acc) 32)
                  acc
                  (let [v' (hmac-sha256 k v)]
                    (recur v' (concat-bytes acc v')))))
            kk (mod-n (bytes->bigint t))]
        (if (and (pos? (.signum kk)) (neg? (.compareTo kk n)))
          kk
          (let [k' (hmac-sha256 k (concat-bytes v (barray 0)))
                v' (hmac-sha256 k' v)]
            (recur k' v')))))))

(defn der-int ^bytes [x]
  (let [b (.toByteArray ^BigInteger (biginteger x))]
    (concat-bytes (byte-array [(byte 0x02) (unchecked-byte (alength b))]) b)))

(defn ecdsa-sign-der-hex
  "DER-hex ECDSA signature of the raw 32-byte k1 challenge (matching GALT's verifier)."
  [^bytes priv-bytes ^bytes msg-bytes]
  (let [d (bytes->bigint priv-bytes)
        e (mod-n (bytes->bigint msg-bytes))
        k (rfc6979-k priv-bytes msg-bytes)
        [rx _] (point-mul k G)
        r (mod-n rx)
        s (mod-n (b* (.modInverse k n) (b+ e (b* r d))))
        s (if (pos? (.compareTo s (.shiftRight n 1))) (b- n s) s)
        body (concat-bytes (der-int r) (der-int s))]
    (assert (not (zero? (.signum r))) "unlucky r=0, retry")
    (assert (not (zero? (.signum s))) "unlucky s=0, retry")
    (bytes->hex (concat-bytes (byte-array [(byte 0x30) (unchecked-byte (alength body))]) body))))

;;; ---------------------------------------------------------------------------
;;; BIP39
;;; ---------------------------------------------------------------------------

(defn load-wordlist [path]
  (let [words (->> (slurp path) str/split-lines (map str/trim) (remove str/blank?) vec)]
    (when-not (= 2048 (count words))
      (throw (ex-info (str "expected 2048 BIP39 words in " path ", got " (count words)) {})))
    words))

(defn normalize-nfkd [s] (Normalizer/normalize ^String s Normalizer$Form/NFKD))

(defn bits-of-byte [b] (for [i (range 7 -1 -1)] (bit-and (bit-shift-right (bit-and b 0xff) i) 1)))

(defn bits->bytes [bits]
  (byte-array (map (fn [chunk] (unchecked-byte (reduce (fn [acc b] (+ (* 2 acc) b)) 0 chunk)))
                   (partition 8 bits))))

(defn entropy->mnemonic
  "BIP39: entropy bytes -> space separated mnemonic using the given wordlist."
  [^bytes entropy words]
  (let [ent-bits (* 8 (alength entropy))
        cs-bits (quot ent-bits 32)
        cs (sha256 entropy)
        bits (take (+ ent-bits cs-bits)
                   (concat (mapcat bits-of-byte entropy)
                           (mapcat bits-of-byte cs)))
        idxs (map #(reduce (fn [acc b] (+ (* 2 acc) b)) 0 %) (partition 11 bits))]
    (str/join " " (map #(nth words %) idxs))))

(defn mnemonic->entropy-bytes
  "Validates a BIP39 mnemonic and returns its entropy bytes."
  [mnemonic words]
  (let [ws (str/split (str/trim mnemonic) #"\s+")]
    (when-not (#{12 15 18 21 24} (count ws))
      (throw (ex-info (str "invalid mnemonic word count: " (count ws)) {})))
    (let [idxs (mapv (fn [w]
                       (let [i (.indexOf ^java.util.List words w)]
                         (when (neg? i) (throw (ex-info (str "unknown BIP39 word: " w) {})))
                         i))
                     ws)
          bits (mapcat (fn [i] (for [b (range 10 -1 -1)] (bit-and (bit-shift-right i b) 1))) idxs)
          total (count bits)
          cs-bits (quot total 33)
          ent-bits (- total cs-bits)
          entropy (bits->bytes (take ent-bits bits))
          expected-cs (take cs-bits (mapcat bits-of-byte (sha256 entropy)))]
      (when-not (= (drop ent-bits bits) expected-cs)
        (throw (ex-info "mnemonic checksum mismatch" {})))
      entropy)))

(defn mnemonic->seed
  "BIP39: mnemonic (+ optional passphrase) -> 64-byte seed."
  [mnemonic passphrase]
  (let [mn (normalize-nfkd (str/join " " (str/split (str/trim mnemonic) #"\s+")))
        salt (normalize-nfkd (str "mnemonic" (or passphrase "")))]
    (pbkdf2-sha512 (utf8 mn) (utf8 salt) 2048 64)))

;;; ---------------------------------------------------------------------------
;;; BIP32 + LUD-05 linking key derivation
;;; ---------------------------------------------------------------------------

(def hardened 0x80000000)

(defn bip32-master [^bytes seed]
  (let [i (hmac-sha512 (utf8 "Bitcoin seed") seed)
        key (bytes->bigint (subarray i 0 32))
        chain-code (subarray i 32 64)]
    (when (or (zero? (.signum key)) (>= (.compareTo key n) 0))
      (throw (ex-info "invalid BIP32 master key" {})))
    {:key key :chain-code chain-code}))

(defn bip32-ckd-priv [{:keys [key chain-code]} ^long index]
  (let [data (if (>= index hardened)
               (concat-bytes (barray 0) (bigint->bytes key 32) (bigint->bytes (BigInteger/valueOf index) 4))
               (concat-bytes (pubkey-compressed-bytes key) (bigint->bytes (BigInteger/valueOf index) 4)))
        i (hmac-sha512 chain-code data)
        il (bytes->bigint (subarray i 0 32))
        child (mod-n (b+ il key))]
    (when (zero? (.signum child))
      (throw (ex-info "invalid BIP32 child key" {})))
    {:key child :chain-code (subarray i 32 64)}))

(defn bip32-derive [master path]
  (reduce bip32-ckd-priv master path))

(defn lud05-linking-node
  "LUD-05 BIP32 linking key for domain (FQDN without trailing dot / port)."
  [master domain]
  (let [hashing-node (bip32-derive master [(+ hardened 138) 0])
        material (hmac-sha256 (bigint->bytes (:key hashing-node) 32) (utf8 domain))
        idxs (mapv (fn [o] (.longValue (bytes->bigint (subarray material o (+ o 4))))) [0 4 8 12])]
    (bip32-derive master (into [(+ hardened 138)] idxs))))

;;; ---------------------------------------------------------------------------
;;; bech32
;;; ---------------------------------------------------------------------------

(def bech32-charset "qpzry9x8gf2tvdw0s3jn54khce6mua7l")

(defn bech32-polymod [values]
  (let [gen [0x3b6a57b2 0x26508e6d 0x1ea119fa 0x3d4233dd 0x2a1462b3]]
    (loop [chk 1 vs (seq values)]
      (if (empty? vs)
        chk
        (let [v (first vs)
              b (bit-shift-right chk 25)
              chk (bit-xor (bit-shift-left (bit-and chk 0x1ffffff) 5) v)
              chk (reduce (fn [c i] (if (bit-test b i) (bit-xor c (nth gen i)) c)) chk (range 5))]
          (recur chk (rest vs)))))))

(defn bech32-hrp-expand [hrp]
  (concat (map #(bit-shift-right (int %) 5) hrp) [0] (map #(bit-and (int %) 31) hrp)))

(defn bech32-decode [^String bech]
  (let [bech (if (or (= bech (str/lower-case bech)) (= bech (str/upper-case bech)))
               (str/lower-case bech)
               (throw (ex-info "mixed-case bech32 string" {})))
        pos (.lastIndexOf bech "1")]
    (when (< pos 1)
      (throw (ex-info "invalid bech32 string" {})))
    (let [hrp (subs bech 0 pos)
          data (mapv (fn [c]
                       (let [i (.indexOf bech32-charset (int c))]
                         (when (neg? i) (throw (ex-info (str "invalid bech32 char: " c) {})))
                         i))
                     (subs bech (inc pos)))]
      (when-not (= 1 (bech32-polymod (concat (bech32-hrp-expand hrp) data)))
        (throw (ex-info "invalid bech32 checksum" {})))
      {:hrp hrp :data (subvec data 0 (- (count data) 6))})))

(defn convertbits-5-8 [data]
  (loop [vs (seq data) acc 0 bits 0 out []]
    (if (empty? vs)
      out
      (let [acc (+ (bit-shift-left acc 5) (first vs))
            bits (+ bits 5)]
        (if (>= bits 8)
          (let [shift (- bits 8)]
            (recur (rest vs)
                   (bit-and acc (dec (bit-shift-left 1 shift)))
                   shift
                   (conj out (bit-and (bit-shift-right acc shift) 0xff))))
          (recur (rest vs) acc bits out))))))

(defn decode-lnurl [s]
  (let [{:keys [hrp data]} (bech32-decode s)]
    (when-not (= "lnurl" hrp)
      (throw (ex-info (str "expected lnurl bech32, got hrp=" hrp) {})))
    (String. (byte-array (map unchecked-byte (convertbits-5-8 data))) StandardCharsets/UTF_8)))

;;; ---------------------------------------------------------------------------
;;; URL / HTTP
;;; ---------------------------------------------------------------------------

(defn parse-query [raw]
  (if (str/blank? raw)
    {}
    (into {} (for [pair (str/split raw #"&")
                   :when (seq pair)]
               (let [[k v] (str/split pair #"=" 2)]
                 [(URLDecoder/decode k "UTF-8") (URLDecoder/decode (or v "") "UTF-8")])))))

(defn append-params [url params]
  (str url
       (if (str/includes? url "?") "&" "?")
       (str/join "&" (for [[k v] params]
                       (str (URLEncoder/encode (str k) "UTF-8") "=" (URLEncoder/encode (str v) "UTF-8"))))))

;;; ---------------------------------------------------------------------------
;;; seeds + config
;;; ---------------------------------------------------------------------------

(defn user->seed-bytes
  "Resolve a config user to a 64-byte BIP39 seed.
   :hex-seed is 16/20/24/28/32 bytes of BIP39 entropy; :mnemonic is a phrase."
  [words {:keys [hex-seed mnemonic passphrase]}]
  (cond
    (and hex-seed mnemonic)
    (throw (ex-info "user config must not set both :hex-seed and :mnemonic" {}))

    hex-seed
    (let [entropy (hex->bytes hex-seed)]
      (when-not (#{16 20 24 28 32} (alength entropy))
        (throw (ex-info ":hex-seed must be 16/20/24/28/32 bytes of BIP39 entropy" {})))
      (mnemonic->seed (entropy->mnemonic entropy words) passphrase))

    mnemonic
    (do (mnemonic->entropy-bytes mnemonic words)
        (mnemonic->seed mnemonic passphrase))

    :else
    (throw (ex-info "user config needs either :hex-seed or :mnemonic" {}))))

(defn script-dir []
  (if-let [f *file*]
    (str (fs/parent (fs/absolutize f)))
    "."))

(defn load-config [path]
  (when-not (fs/exists? path)
    (throw (ex-info (str "config file not found: " path) {})))
  (edn/read-string (slurp path)))

;;; ---------------------------------------------------------------------------
;;; CLI
;;; ---------------------------------------------------------------------------

(def usage
  (str/join
   \newline
   ["Usage:"
    "  bb scripts/lnurl-login.bb <user> <lnurl>"
    "  bb scripts/lnurl-login.bb --list"
    "  bb scripts/lnurl-login.bb --generate-hex-seed"
    "  bb scripts/lnurl-login.bb --generate-mnemonic"
    ""
    "Options:"
    "  --config PATH    EDN config (default: scripts/lnurl-login.edn)"
    "  --wordlist PATH  BIP39 wordlist (default: scripts/bip39-english.txt)"
    "  --verbose        print the full callback URL"]))

(defn parse-args [args]
  (loop [args args
         opts {:config nil :wordlist nil :list false
               :generate-hex-seed false :generate-mnemonic false :verbose false}
         positional []]
    (if (empty? args)
      [opts positional]
      (let [a (first args)]
        (case a
          "--config" (recur (rest (rest args)) (assoc opts :config (second args)) positional)
          "--wordlist" (recur (rest (rest args)) (assoc opts :wordlist (second args)) positional)
          "--list" (recur (rest args) (assoc opts :list true) positional)
          "--generate-hex-seed" (recur (rest args) (assoc opts :generate-hex-seed true) positional)
          "--generate-mnemonic" (recur (rest args) (assoc opts :generate-mnemonic true) positional)
          "--verbose" (recur (rest args) (assoc opts :verbose true) positional)
          "--help" (do (println usage) (System/exit 0))
          "-h" (do (println usage) (System/exit 0))
          (recur (rest args) opts (conj positional a)))))))

(defn do-login [opts user lnurl config words]
  (let [entry (or (get-in config [:users user])
                  (throw (ex-info (str "unknown user: " user " (try --list)") {})))
        callback (decode-lnurl lnurl)
        uri (URI. callback)
        host (str/lower-case (or (.getHost uri) ""))
        params (parse-query (.getRawQuery uri))
        k1 (get params "k1")]
    (when (str/blank? host)
      (throw (ex-info "could not parse host from LNURL" {:url callback})))
    (when-not (and k1 (= "login" (get params "tag")))
      (throw (ex-info "not an LNURL-auth login request (need tag=login and k1)"
                      {:params params})))
    (let [seed (user->seed-bytes words entry)
          linking (lud05-linking-node (bip32-master seed) host)
          priv (:key linking)
          pub-hex (pubkey-compressed-hex priv)
          sig-hex (ecdsa-sign-der-hex (bigint->bytes priv 32) (hex->bytes k1))
          url (append-params callback {"sig" sig-hex "key" pub-hex})
          resp (http/get url {:throw false :timeout 15000})
          body (str/trim (str (:body resp)))
          parsed (try (json/parse-string body true) (catch Exception _ nil))]
      (println (str "user:           " user))
      (println (str "domain:         " host))
      (println (str "linking pubkey: " pub-hex))
      (when (:verbose opts)
        (println (str "callback:       " url)))
      (println (str "HTTP status:    " (:status resp)))
      (println (str "response:       " body))
      (if (and (= 200 (:status resp)) (= "OK" (:status parsed)))
        (println (str "\nLogged in as " user "."))
        (do (binding [*out* *err*] (println "\nLNURL-auth failed."))
            (System/exit 1))))))

(defn -main [& args]
  (let [[opts positional] (parse-args args)
        dir (script-dir)
        config-path (or (:config opts) (str dir "/lnurl-login.edn"))
        wordlist-path (or (:wordlist opts) (System/getenv "BIP39_WORDLIST") (str dir "/bip39-english.txt"))]
    (cond
      (:generate-hex-seed opts)
      (println (bytes->hex (random-bytes 32)))

      (:generate-mnemonic opts)
      (println (entropy->mnemonic (random-bytes 32) (load-wordlist wordlist-path)))

      (:list opts)
      (let [config (load-config config-path)]
        (if (seq (:users config))
          (doseq [[k v] (sort-by key (:users config))]
            (println (str k "\t" (cond (:mnemonic v) "(mnemonic)"
                                       (:hex-seed v) "(hex-seed)"
                                       :else "(no seed)"))))
          (println "no users configured")))

      (= 2 (count positional))
      (let [[user lnurl] positional]
        (do-login opts user lnurl (load-config config-path) (load-wordlist wordlist-path)))

      :else
      (do (println usage) (System/exit 1)))))

(when (= *file* (System/getProperty "babashka.file"))
  (try
    (apply -main *command-line-args*)
    (catch Exception e
      (binding [*out* *err*]
        (println (str "Error: " (.getMessage e))))
      (System/exit 1))))

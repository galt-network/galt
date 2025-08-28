(ns galt.core.infrastructure.name-generator
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str])
  (:import
    [java.security MessageDigest]
    [java.math BigInteger]))

(defn sha256
  "Compute SHA256 hash of input string, return as hexadecimal string."
  [input]
  (let [md (MessageDigest/getInstance "SHA-256")
        bytes (.digest md (.getBytes input "UTF-8"))]
    (format "%064x" (BigInteger. 1 bytes))))

(defn hash-to-index
  "Generate a deterministic index in [0, range-size) based on SHA256 hash of input."
  [input range-size]
  (let [hash (sha256 input)
        ;; Take first 8 characters (4 bytes) of hex string for simplicity
        prefix (subs hash 0 8)
        ;; Convert hex prefix to integer
        num (BigInteger. prefix 16)]
    (mod num range-size)))

(defn read-file [file-name]
  (slurp (io/resource file-name)))

(defn read-word-list [file-name]
  (with-open [rdr (io/reader (io/resource file-name))]
    (->> (line-seq rdr)
         (map str/trim)
         (filter not-empty)
         vec)))

(defn group-by-first-letter [words]
  (->> words
       (group-by #(str/lower-case (first %)))
       (map (fn [[k v]] [(keyword k) v]))
       (into {})))

(defonce names-db (atom nil))

(defn pre-populate-db
  []
  (let [nouns (read-word-list "english_nouns.txt")
        adjectives (read-word-list "english_adjectives.txt")]
  {:adjectives adjectives
   :nouns-grouped-by-first-letter (group-by-first-letter nouns)}))

(defn generate-username [db input]
  (let [adjectives (get db :adjectives [])
        noun-map (get db :nouns-grouped-by-first-letter {})
        normalized-input (str/lower-case input)
        adjective-count (count adjectives)
        adjective-by-hash (nth adjectives (hash-to-index normalized-input adjective-count))
        adjective-first-letter (str/lower-case (first adjective-by-hash))
        nouns-with-adjective-letter (get noun-map (keyword adjective-first-letter))
        noun-by-hash (nth nouns-with-adjective-letter
                          (hash-to-index normalized-input (count nouns-with-adjective-letter)))]
    (str (str/capitalize adjective-by-hash) " " (str/capitalize noun-by-hash))))

(defn generate
  ([input] (generate names-db input))
  ([db input]
    (when nil? @db (reset! db (pre-populate-db)))
    (generate-username @db input)))

(comment
  (hash-to-index "hello long text" 1000)
  (take 10 (read-word-list "english_nouns.txt"))
  (type (read-file "english_nouns.txt"))
  (println "Name for Claudia's public key is" (generate "02938409582743085209348075928347059827340958720394875"))
  (println "Name for Madis's public key is" (generate "298347982374982374lakjshdlkfjhalkdsjh"))
  (let [inputs ["user1" "user2" "user1" "test" "randomness" "things" "madis" "Madis"]]
      (doseq [input inputs]
        (println (format "Input: %s, Name: %s" input (generate input))))))

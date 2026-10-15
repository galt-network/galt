(ns galt.connect.adapters.memory-search-repository
  (:require
    [clojure.string :as str]
    [galt.connect.domain.search-repository :as repo]
    [java-time.api :as jt]))

;; shortcut: this stand-in only strips a trailing s, Postgres stems with the
;; English configuration; keep fixture stems obviously equal, never read this
;; as stemmer parity.
(defn- stem
  [word]
  (str/replace word #"s$" ""))

(defn- words
  [text]
  (map stem (re-seq #"[a-z0-9]+" (str/lower-case (str text)))))

(defn- matches-query?
  [hit query]
  (or (str/blank? query)
      (let [q (seq (words query))]
        (and q (every? (into #{} (concat (words (:title hit)) (words (:description hit)))) q)))))

(defn- in-text?
  [text q]
  (every? (into #{} (words text)) q))

(defn- window
  [text q]
  (let [qs (set q)
        tokens (vec (re-seq #"\S+" (str text)))
        i (first (keep-indexed (fn [i token] (when (qs (first (words token))) i)) tokens))]
    (when (and i (seq tokens))
      (let [start (max 0 (- i 4))
            end (min (count tokens) (+ i 5))]
        (str (when (pos? start) "… ")
             (str/join " " (subvec tokens start end))
             (when (< end (count tokens)) " …"))))))

(defn- annotate
  [hit q]
  (assoc hit
         :match (if (in-text? (:title hit) q) :title :body)
         :snippet (or (window (:description hit) q) (window (:title hit) q))))

(def all-types #{:member :group :event :post})

(defn- in-range?
  [t from to]
  (and t
       (not (jt/before? t from))
       (jt/before? t to)))

(defn- matches-period?
  [hit from to]
  (or (nil? from) (nil? to)
      (case (:type hit)
        (:member :group) true
        :event (in-range? (:start-time hit) from to)
        :post (in-range? (:publish-at hit) from to)
        true)))

(defn- haversine-km
  [lat1 lng1 lat2 lng2]
  (let [r 6371.0
        dlat (Math/toRadians (- lat2 lat1))
        dlng (Math/toRadians (- lng2 lng1))
        a (+ (Math/pow (Math/sin (/ dlat 2)) 2)
             (* (Math/cos (Math/toRadians lat1))
                (Math/cos (Math/toRadians lat2))
                (Math/pow (Math/sin (/ dlng 2)) 2)))]
    (* r 2 (Math/asin (Math/sqrt a)))))

(defn- matches-location?
  [hit location]
  (or (nil? location)
      (let [{:keys [lat lng radius-km]} location]
        (and (:lat hit) (:lng hit)
             (<= (haversine-km lat lng (:lat hit) (:lng hit))
                 (or radius-km 100))))))

(defn new-memory-search-repository
  [hits]
  (reify repo/SearchRepository
    (candidates [_ {:keys [query types from to location]}]
      (let [wanted (if (seq types) types all-types)
            q (seq (words query))]
        (->> hits
             (remove :hidden?)
             (remove #(and (= :member (:type %)) (:deleted? %)))
             (filter #(contains? wanted (:type %)))
             (filter #(matches-query? % query))
             (filter #(matches-period? % from to))
             (filter #(matches-location? % location))
             (map #(if q (annotate % q) %)))))))

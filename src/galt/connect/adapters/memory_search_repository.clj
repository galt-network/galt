(ns galt.connect.adapters.memory-search-repository
  (:require
    [clojure.string :as str]
    [galt.connect.domain.search-repository :as repo]
    [java-time.api :as jt]))

(defn- matches-query?
  [hit query]
  (or (str/blank? query)
      (let [q (str/lower-case query)]
        (some #(and % (str/includes? (str/lower-case %) q))
              [(:title hit) (:description hit)]))))

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
      (let [wanted (if (seq types) types all-types)]
        (filter (fn [hit]
                  (and (not (:hidden? hit))
                       (contains? wanted (:type hit))
                       (matches-query? hit query)
                       (matches-period? hit from to)
                       (matches-location? hit location)))
                hits)))))

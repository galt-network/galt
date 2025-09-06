(ns galt.locations.adapters.pg-location-repository
  (:require
    [galt.locations.domain.location-repository :as lr :refer [LocationRepository]]
    [galt.core.adapters.db-access :refer [query in-transaction]]
    [galt.locations.domain.entities.city :as city]
    [galt.locations.domain.entities.country :as country]
    [galt.locations.domain.entities.location :as location]
    [galt.core.adapters.db-result-transformations :refer [transform-row defaults]]
    ))

(def city-spec
  {:cities/id defaults
   :cities/name defaults
   :cities/latitude defaults
   :cities/longitude defaults
   :cities/country_code defaults})

(def country-spec
  {:countries/id defaults
   :countries/name defaults
   :countries/latitude defaults
   :countries/longitude defaults
   :countries/iso2 [(constantly :code) identity]
   :countries/emoji defaults})

(def location-spec
  {:locations/id defaults
   :locations/name defaults
   :locations/latitude defaults
   :locations/longitude defaults
   :locations/country_code defaults
   :locations/city_id defaults})

(def common-city-columns
  [:cities/id
   :cities/name
   :cities/latitude
   :cities/longitude
   :cities/country_code])

(def common-country-columns
  [:countries/id
   :countries/name
   :countries/latitude
   :countries/longitude
   :countries/iso2
   :countries/emoji])

(defn cities-fuzzy-query
  [s limit similarity]
  {:select (conj common-city-columns [[:word_similarity [:lower s] [:lower :name]] :similarity])
    :from [:cities]
    :where [:or
            [:% [:lower :name] [:lower s]]
            [:> [:word_similarity [:lower s] [:lower :name]] similarity]]
    :order-by [[[:word_similarity [:lower s] [:lower :name]] :desc]]
    :limit limit})

(defn countries-fuzzy-query
  [s limit similarity]
  {:select (conj common-country-columns [[:word_similarity [:lower s] [:lower :name]] :similarity])
    :from [:countries]
    :where [:or
            [:% [:lower :name] [:lower s]]
            [:> [:word_similarity [:lower s] [:lower :name]] similarity]]
    :order-by [[[:word_similarity [:lower s] [:lower :name]] :desc]]
    :limit limit})

(defn add-country-code-to-where
  [country-code query]
  (assoc query :where [:and [:= :country_code country-code] (:where query)]))

(def default-result-limit 10)
(def default-word-similarity-threshold 0.1)

(defn map-without-nils
  [maplike]
  (into {} (remove (comp nil? val) maplike)))

(defrecord PgLocationRepository [db-access]
  LocationRepository
  (find-city-by-id [_ id]
    (->> {:select common-city-columns
          :from [:cities]
          :where [:= :id id]}
         (query db-access ,,,)
         (first ,,,)
         (transform-row city-spec ,,,)
         (city/map->City ,,,)))

  (fuzzy-find-city [_ s]
    (->> (cities-fuzzy-query s default-result-limit default-word-similarity-threshold)
         (query db-access ,,,)
         (map #(transform-row city-spec %) ,,,)
         (map city/map->City ,,,)))

  (fuzzy-find-city [_ s country-code]
    (->> (cities-fuzzy-query s default-result-limit default-word-similarity-threshold)
         (add-country-code-to-where country-code ,,,)
         (query db-access ,,,)
         (map #(transform-row city-spec %) ,,,)
         (map city/map->City ,,,)))

  (all-countries [_]
    (->> {:select common-country-columns :from [:countries]}
         (query db-access ,,,)
         (map #(transform-row country-spec %) ,,,)
         (map country/map->Country ,,,)))

  (fuzzy-find-country [_ s]
    (->> (countries-fuzzy-query s default-result-limit default-word-similarity-threshold)
         (query db-access ,,,)
         (map #(transform-row country-spec %) ,,,)
         (map country/map->Country ,,,)))

  (find-country-by-code [_ code]
    (println ">>> PgLocationRepository#find-country-by-code " code)
    (->> {:select common-country-columns
          :from [:countries]
          :where [:= :iso2 code]}
         (query db-access ,,,)
         (first ,,,)
         (transform-row country-spec ,,,)
         (country/map->Country ,,,)))

  (add-location [_ location]
    (->> (query db-access {:insert-into [:locations] :values [(map-without-nils location)] :returning [:*]})
         (first ,,,)
         (transform-row location-spec ,,,)
         (location/map->Location ,,,)))

  (find-location-by-id [_ id]
    (->> {:select [:*]
          :from [:locations]
          :where [:= :id id]}
         (query db-access ,,,)
         (first ,,,)
         (transform-row location-spec ,,,)
         (location/map->Location ,,,))))

(def last-repo (atom nil))

(defn new-location-repository
  [db-access]
  (reset! last-repo (PgLocationRepository. db-access))
  (PgLocationRepository. db-access))

(comment
  (lr/fuzzy-find-city @last-repo "New")
  (lr/fuzzy-find-city @last-repo "New" "GB")
  (lr/find-city-by-id @last-repo 31740)
  (lr/fuzzy-find-country @last-repo "New")
  (lr/find-country-by-code @last-repo "SV")
  (lr/all-countries @last-repo)

  (def my-location (location/map->Location {:name "Best Place"
                                            :latitude 13.48833
                                            :longitude -89.32222
                                            :city-id 104884
                                            :country-code "SV"}))
  (lr/add-location @last-repo my-location)
  (lr/find-location-by-id @last-repo 3)
  )

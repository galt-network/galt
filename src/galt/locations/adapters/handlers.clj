(ns galt.locations.adapters.handlers
  (:require
    [galt.core.infrastructure.web.helpers :refer [get-signals]]
    [galt.core.adapters.sse-helpers :refer [with-sse]]
    [galt.locations.domain.location-repository :as lr]
    [galt.core.views.location :as location-views]))

(defn search-cities
  [{:keys [location-repo]} req]
  (let [signals (get-signals req)
        query-from-signals (get signals :search)
        query-from-params (get-in req [:params :query])
        query (or query-from-signals query-from-params)
        country-code (get signals :country-code)
        filtered-cities (if (seq country-code)
                          (lr/fuzzy-find-city location-repo query country-code)
                          (lr/fuzzy-find-city location-repo query))
        dropdown-items (map (fn [city]
                              (location-views/dropdown-item
                                {
                                 ; :text (str (:name city) " (" (:country-code city) ")")
                                 :name (:name city)
                                 :extra (:country-code city)
                                 :value (:id city)}))
                            filtered-cities)]
    (with-sse req
      (fn [send!]
        (send! :html (location-views/dropdown-content dropdown-items))
        (send! :signals {:show-results true})))))

(defn coordinates
  [{:keys [location-repo]} req]
  (let [signals (get-signals req)
        type (get-in req [:query-params "type"])
        entity (case type
                 "city" (lr/find-city-by-id
                          location-repo
                          (Integer/parseInt (get-in req [:query-params "id"])))
                 "country" (lr/find-country-by-code
                             location-repo
                             (:country-code signals)))]
    (with-sse req
      (fn [send!]
        (send! :signals {:latitude (:latitude entity) :longitude (:longitude entity)})
        (when (= type "city") (send! :signals {:country-code (:country-code entity)
                                               :city-id (:id entity)}))
        (when (= type "country") (send! :signals {:search ""}))))))

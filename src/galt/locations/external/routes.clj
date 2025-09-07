(ns galt.locations.external.routes
  (:require
    [reitit.ring :as rr]
    [galt.locations.adapters.handlers :as location]))

(defn router
 [deps]
 (rr/router
   [["/locations/search-cities" {:name :locations/cities
                                 :get (partial location/search-cities deps)}]
    ["/locations/coordinates" {:name :locations/coordinates
                               :get (partial location/coordinates deps)}]]
   {:reitit.middleware/registry (:reitit-middleware deps)}))

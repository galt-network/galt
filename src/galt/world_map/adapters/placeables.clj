(ns galt.world-map.adapters.placeables
  "Per-user PlaceableObjectProvider for globo: anonymous users get a
  restricted model list, everyone else the full list."
  (:require
   [clojure.string :as str]
   [is.galt.globo.protocols :as protocols]
   [is.galt.globo.server.placeables :as placeables]))

(defn anon-user-id?
  "Anonymouse globo user-ids are prefixed `anon-` by wrap-globo-user-id."
  [user-id]
  (str/starts-with? (str user-id) "anon-"))

(def restricted-config
  "Model ids offered to anonymous users."
  (->> placeables/default-config
       (filter (fn [{:keys [model-id]}]
                 (#{"carrot" "tree" "ancap-flag"} model-id)))
       vec))

(defrecord PerUserPlaceables [base-config restricted]
  protocols/PlaceableObjectProvider
  (placeable-objects [_ user-id]
    (if (anon-user-id? user-id) restricted base-config)))

(defn per-user-placeables
  ([] (->PerUserPlaceables placeables/default-config restricted-config))
  ([base-config restricted]
   (->PerUserPlaceables base-config restricted)))

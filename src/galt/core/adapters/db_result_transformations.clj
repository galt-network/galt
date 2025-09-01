(ns galt.core.adapters.db-result-transformations
  (:require
    [camel-snake-kebab.core :as csk]))

(defn without-namespace [k] (keyword (name k)))

(defn ->local-date-time [v] (.toLocalDateTime v))

(def non-namespaced-kebab (comp csk/->kebab-case without-namespace))

(def defaults [non-namespaced-kebab identity])


(defn transform-row
  "Takes a map where the keys are database column names (namespaced keywords)
  and values are vectors of function pairs [key-transform, value-transform]

  It goes over all the key-val pairs in the row map and returns new map with
  keys returned by calling key-transform with the original key and values by
  value-transform.

  Value transform can be missing and identity will be used as default"
  [row-spec row]
  (reduce (fn [new-row [column-key [key-fn val-fn]]]
            (assoc new-row (key-fn column-key) ((or val-fn identity) (column-key row))))
          {}
          row-spec))

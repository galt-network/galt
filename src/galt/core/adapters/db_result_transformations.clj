(ns galt.core.adapters.db-result-transformations
  (:require
    [camel-snake-kebab.core :as csk]))

(defn without-namespace [k] (keyword (name k)))

(defn ->local-date-time [v] (.toLocalDateTime v))

(def non-namespaced-kebab (comp csk/->kebab-case without-namespace))

(def defaults [non-namespaced-kebab identity])
(def default-datetime [non-namespaced-kebab ->local-date-time])

(defn map-without-nils
  [maplike]
  (into {} (remove (comp nil? val) maplike)))

(defn transform-row
  "Takes a map where the keys are database column names (namespaced keywords)
  and values are vectors of function pairs [key-transform, value-transform]

  It goes over all the key-val pairs in the row map and returns new map with
  keys returned by calling key-transform with the original key and values by
  value-transform.

  Value transform can be missing and identity will be used as default"
  [row-spec row]
  (when row
    (reduce (fn [new-row [column-key [key-fn val-fn]]]
              (let [key-fn (if (keyword? key-fn) (constantly key-fn) key-fn)]
                (assoc new-row
                       (key-fn column-key)
                       (when (column-key row) ((or val-fn identity) (column-key row))))))
            {}
            row-spec)))

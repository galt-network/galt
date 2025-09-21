(ns galt.core.views.table
  (:require
    [clojure.string :as str]))

(defn- maybe-wrap-link?
  [href el]
  (if href
    [:a {:href href} el]
    el))

(defn column-key
  [column]
  (if (and (vector? column) (= 2 (count column)))
    (second column)
    column))

(defn row
  [column-processor columns row]
  [:tr (map (fn [column]
              (let [column-key (column-key column)
                    processor (get column-processor column-key)
                    column-data (get row column-key)
                    after-processing (or
                                       (get column-processor (keyword (str"after-" (name column-key))))
                                       identity)]
                (if processor
                  [:td (after-processing (processor column-data))]
                  [:td (maybe-wrap-link? (:href row) (get row column-key))]))) columns)])


(defn- column-title
  [column]
  (if (and (vector? column) (= 2 (count column)))
    (first column)
    (str/capitalize (name column))))

(defn table
  "columns - vector of keywords [:column-a, :column-b] or pairs [[\"Column Title\" :column-key]]
  column-processor - map of {:actions (fn [])}"
  [{:keys [columns column-processor rows]}]
  [:table {:class [:table :is-striped :is-hoverable :is-fullwidth]}
   [:thead
    [:tr (map #(conj [:th] (column-title %)) columns)]]
    [:tbody
     (map (partial row column-processor columns) rows)]])

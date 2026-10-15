(ns galt.connect.domain.rank
  (:require
    [clojure.string :as str]))

(def ^:private match-order {:title 0 :body 1})

(defn rank
  [hits {:keys [query]}]
  (let [by-activity (sort-by :activity-at #(compare %2 %1) hits)]
    (if (str/blank? query)
      by-activity
      (sort-by #(get match-order (:match %) (count match-order)) by-activity))))

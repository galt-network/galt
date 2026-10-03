(ns galt.connect.domain.rank)

(defn rank
  [hits _criteria]
  (sort-by :activity-at #(compare %2 %1) hits))

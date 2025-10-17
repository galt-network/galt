(ns galt.groups.domain.use-cases.list-groups)

(defn map-group-to-location
  [groups locations]
  (let [location-id->location (reduce (fn [acc loc] (assoc acc (:id loc) loc)) {} locations)]
    (reduce (fn [acc group]
              (assoc acc (:id group) (get location-id->location (:location-id group)))) {} groups)))

(defn list-groups-use-case
  [{:keys [list-groups locations-by-id]}
   {:keys [query limit offset]}]
  (let [groups (list-groups {:query query :limit limit :offset offset})
        locations (locations-by-id (map :location-id groups))]
    [:ok {:groups groups
          :locations (map-group-to-location groups locations)}]))

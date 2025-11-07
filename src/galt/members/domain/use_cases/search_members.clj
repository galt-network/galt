(ns galt.members.domain.use-cases.search-members)

(defn search-members-use-case
  [{:keys [list-members find-groups-by-member locations-by-id]} {:keys [query offset group-id]}]
  (let [members (list-members {:query query :group-id group-id :offset offset})]
    [:ok {:members members
          :locations (reduce (fn [acc loc] (assoc acc (:id loc) loc))
                             {}
                             (locations-by-id (map :location-id members)))
          :groups (reduce (fn [acc m] ; FIXME refactor to avoid N+1
                            (assoc acc (:id m) (find-groups-by-member (:id m))))
                          {}
                          members)}]))

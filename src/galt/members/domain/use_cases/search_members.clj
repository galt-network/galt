(ns galt.members.domain.use-cases.search-members)

(defn search-members-use-case
  [{:keys [find-members-by-name list-members find-groups-by-member]} {:keys [query group-id]}]
  (let [members (find-members-by-name query group-id)]
    [:ok {:members members
          :groups (reduce (fn [acc m]
                            (assoc acc (:id m) (find-groups-by-member (:id m))))
                          {}
                          members)}]))

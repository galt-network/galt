(ns galt.members.domain.use-cases.search-members)

(defn search-members-use-case
  [{:keys [fuzzy-find-member list-members find-groups-by-member]} {:keys [query]}]
  (let [members (if (> 3 (count query)) (list-members) (fuzzy-find-member query))]
    [:ok {:members members
          :groups (reduce (fn [acc m]
                            (assoc acc (:id m) (find-groups-by-member (:id m))))
                          {}
                          members)}]))

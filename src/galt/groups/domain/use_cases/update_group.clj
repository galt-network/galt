(ns galt.groups.domain.use-cases.update-group)

(defn update-group-use-case
  [{:keys [find-group-by-id]} command]
  (let [group (:group command)
        db-group (find-group-by-id (:id group))]
    [:ok db-group]))

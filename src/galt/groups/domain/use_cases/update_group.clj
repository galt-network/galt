(ns galt.groups.domain.use-cases.update-group
  (:require
    [galt.groups.domain.group-repository :as gr]
    ))

(defn update-group-use-case
  [{:keys [group-repo location-repo]} command]
  (let [group (:group command)
        db-group (gr/find-group-by-id group-repo (:id group))]
    [:ok db-group]))

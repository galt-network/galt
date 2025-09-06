(ns galt.groups.domain.use-cases.delete-group
  (:require
    [galt.groups.domain.group-repository :as gr :refer [delete-group]]))

(defn delete-group-use-case
  [{:keys [group-repo]} {:keys [deletor-id group-id]}]
  (let []
    ; TODO check that the user is allowed to delete group
    (gr/delete-group group-repo group-id)))

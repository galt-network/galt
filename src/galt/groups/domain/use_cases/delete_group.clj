(ns galt.groups.domain.use-cases.delete-group)

(defn delete-group-use-case
  [{:keys [delete-group]} {:keys [deletor-id group-id]}]
  (let []
    ; TODO check that the user is allowed to delete group
    (delete-group group-id)))

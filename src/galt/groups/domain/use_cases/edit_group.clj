(ns galt.groups.domain.use-cases.edit-group
  (:require
    [galt.groups.domain.group-repository :as gr]
    [galt.groups.domain.entities.group-membership :refer [allowed-action?]]))

(defn edit-group-use-case
  [{:keys [find-group-by-id find-membership-by-member]}
   {:keys [group-id editor-id]}]
  (let [membership (find-membership-by-member group-id editor-id)]
    (if (allowed-action? membership :edit)
      [:ok (find-group-by-id group-id)]
      [:error {:message "User is not allowed to edit this group"}])))

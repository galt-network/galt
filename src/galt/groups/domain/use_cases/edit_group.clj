(ns galt.groups.domain.use-cases.edit-group
  (:require
    [galt.groups.domain.group-repository :as gr]
    [galt.groups.domain.entities.group-membership :refer [allowed-action?]]))

(defn edit-group-use-case
  [{:keys [group-repo]}
   {:keys [group-id editor-id]}]
  (let [membership (gr/find-membership-by-member group-repo group-id editor-id)]
    (if (allowed-action? membership :edit)
      [:ok (gr/find-group-by-id group-repo group-id)]
      [:error {:message "User is not allowed to edit this group"}])))

(ns galt.groups.adapters.view-models
  (:require
   [galt.core.adapters.db-access :refer [query]]
   [galt.core.adapters.link-generator :refer [link-for-route]]
   [galt.groups.domain.group-repository :as gr]))

(defn groups-view-model
  [{:keys [group-repo db-access]} req]
  (let [query-term (get-in req [:params :query])
        groups (gr/find-groups-by-name group-repo query-term)
        group-counts {:select [:group_id [[:count :*] :members_count]]
                      :from [:group_memberships]
                      :group-by [:group_id]
                      :where [:in :group-id (map :id groups)]}
        counts (reduce
                 (fn [acc c] (assoc acc (:group_memberships/group_id c) (:members_count c)))
                 {}
                 (query db-access group-counts))
        add-counts (fn [g] (assoc g :members (get counts (:id g))))
        add-action (fn [g] (assoc g :actions
                                  [{:name "View"
                                    :href (link-for-route req :groups/by-id {:id (:id g)})}
                                   {:name "Edit"
                                    :href (link-for-route req :groups/edit-group {:id (:id g)})}]))
        groups-with-extras (->> groups
                                (map add-counts ,,,)
                                (map add-action))]
    {:group-name (get-in req [:params :group-name])
     :group-name-id (get-in req [:params :group-name-id])
     :columns [["Group Name" :name] :description :members :actions]
     :groups groups-with-extras}))

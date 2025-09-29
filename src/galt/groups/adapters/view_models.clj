(ns galt.groups.adapters.view-models
  (:require
   [galt.core.adapters.db-access :refer [query]]
   [galt.core.adapters.link-generator :refer [link-for-route]]
   [galt.groups.domain.group-repository :as gr]))

(defn groups-view-model
  [deps req]
  (let [groups (gr/list-groups (:group-repo deps))
        group-counts {:select [:group_id [[:count :*] :members_count]]
                      :from [:group_memberships]
                      :group-by [:group_id]}
        counts (reduce
                 (fn [acc c] (assoc acc (:group_memberships/group_id c) (:members_count c)))
                 {}
                 (query (:db-access deps) group-counts))
        add-counts (fn [g] (assoc g :members (get counts (:id g))))
        add-action (fn [g] (assoc g :actions
                                  [{:name "View"
                                    :href (link-for-route req :groups/by-id {:id (:id g)})}
                                   {:name "Edit"
                                    :href (link-for-route req :groups/edit-group {:id (:id g)})}]))
        groups-with-extras (->> groups
                                (map add-counts ,,,)
                                (map add-action))]
    {:columns [["Group Name" :name] :description :members :actions]
     :groups groups-with-extras}))

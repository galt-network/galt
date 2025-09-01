(ns galt.members.adapters.view-models
  (:require
    [galt.groups.domain.group-repository :as gr]
    [galt.members.domain.user-repository :refer [list-users]]
    ))

(defn members-model
  [{:keys [user-repo group-repo link-for-route]} _req]
  (let [users (list-users user-repo)
        add-groups (fn [user]
                     (assoc user
                            :groups
                            (map (fn [g] {:name (:groups/name g)
                                          :href (link-for-route :groups/show-group {:id (:groups/id g)})})
                                 (gr/find-groups-by-member group-repo (:users/id user)))))
        add-user (fn [user]
                   (assoc user :user {:name (:users/name user)
                                      :href (link-for-route :members/show-profile {:id (:users/id user)})}))]
    {:column-titles [["Name" :user] ["Member Since" :users/created-at] :groups]
     :users (->> users
                 (map add-user ,,,)
                 (map add-groups ,,,))}))

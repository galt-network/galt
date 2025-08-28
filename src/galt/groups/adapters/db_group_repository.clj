(ns galt.groups.adapters.db-group-repository
  (:require
    [galt.groups.domain.group-repository :as gr :refer [GroupRepository]]
    [galt.core.adapters.db-access :refer [query in-transaction]]))

(defrecord DbGroupRepository [db-access]
  GroupRepository
  (create-group [_ creator-id name description]
    (in-transaction
      db-access
      (fn [query]
        (let [group-id (random-uuid)]
          (query {:insert-into [:groups]
                  :columns [:id :name :description]
                  :values [[group-id name description]]})
          (query {:insert-into [:group_memberships]
                  :columns [:group_id :member_id :role]
                  :values [[group-id creator-id "owner"]]})
          (first (query {:select [:*] :from [:groups] :where [:= :id group-id]}))))))

  (find-group-by-id [_ id]
    (first (query db-access {:select [:*] :from [:groups] :where [:= :id id]})))

  (update-group [_ id name description]
    (query db-access {:update [:groups]
                      :set {:name name :description description}
                      :where [:= :id id]}))

  (add-to-group [_ group-id member-id role]
    (query db-access {:insert-into [:group-memberships]
                      :columns [:group_id :member_id :role]
                      :values [[group-id member-id role]]}))

  (find-groups-by-member [_ member-id]
    (query db-access {:select [:groups.*]
                      :from [:groups]
                      :join [:group_memberships [:= :groups.id :group_memberships.group_id]]
                      :where [:= :group_memberships.member_id member-id]}))

  (list-members [_ group-id {:keys [limit]}]
    (let [sql {:select [:users.*]
               :from [:users]
               :join [:group_memberships [:and
                                          [:= :group_memberships/group_id group-id]
                                          [:= :users/id :group_memberships/member_id]]]}]
      (query db-access
             (cond-> sql
               limit (assoc ,,, :limit limit)))))

  (list-groups [_]
    (query db-access {:select [:*] :from [:groups]})))

(defonce last-repo (atom nil))

(defn new-group-repository [db-access]
  (reset! last-repo (DbGroupRepository. db-access))
  @last-repo)

(comment
  (gr/list-groups @last-repo)
  (gr/create-group @last-repo (parse-uuid "38da67bd-ee75-4a71-b70c-618ac1053ec7") "The First" "First Group Description")
  )

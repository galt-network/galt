(ns galt.groups.adapters.db-group-repository
  (:require
    [galt.groups.domain.group-repository :as gr :refer [GroupRepository]]
    [galt.core.adapters.db-access :refer [query in-transaction]]
    [galt.groups.domain.entities.group :refer [map->Group]]
    [galt.core.adapters.db-result-transformations :refer [transform-row defaults ->local-date-time]]
    [galt.groups.domain.entities.group-membership :refer [map->GroupMembership]]))


(def group-spec
  {:groups/id defaults
   :groups/name defaults
   :groups/description defaults
   :groups/avatar defaults
   :groups/created_at [(first defaults) ->local-date-time]
   :groups/location_id defaults})

(def group-membership-spec
  {:group_memberships/member_id defaults
   :group_memberships/group_id defaults
   :group_memberships/role [(first defaults) keyword]})

(defrecord DbGroupRepository [db-access]
  GroupRepository
  (add-group [_ founder-id group]
    (in-transaction
      db-access
      (fn [query]
        (query {:insert-into [:groups]
                :values [group]})
        (query {:insert-into [:group_memberships]
                :columns [:group_id :member_id :role]
                :values [[(:id group) founder-id "founder"]]})
        (->> {:select [:*] :from [:groups] :where [:= :id (:id group)]}
            query
            first
            (transform-row group-spec)
            map->Group))))

  (find-group-by-id [_ id]
    (->> {:select [:*] :from [:groups] :where [:= :id id] :limit 1}
         (query db-access ,,,)
         (first ,,,)
         (transform-row group-spec ,,,)
         (map->Group ,,,)))

  (find-groups-by-name [_ name group-id]
    (some->> {:select-distinct-on [[:groups.id] :groups.*]
              :from [:groups]
              :join [:group_memberships [:= :group_memberships.group_id :groups.id]]
              :where [:and
                      (if (nil? group-id) [:= 1 1] [:= :group_memberships.group_id group-id])
                      [:ilike :name (str "%" name "%")]]
              :limit 10}
             (query db-access ,,,)
             (map #(transform-row group-spec %) ,,,)
             (map map->Group ,,,)))

  (find-groups-by-founder-id [_ founder-id]
    (query db-access {:select [:groups.*]
                      :from [:groups]
                      :join [:group_memberships [:= :groups.id :group_memberships.group_id]]
                      :where [:and
                              [:= :group_memberships.member_id founder-id]
                              [:= :group_memberships.role "founder"]]}))

  (fuzzy-find-group [_ s]
    (gr/fuzzy-find-group _ s nil))

  (fuzzy-find-group [_ s member-id]
    (let [limit 10
          similarity-threshold 0.2]
      (->> {:select [:groups.*]
            :from [:groups]
            :where [:or
                    [:% [:lower :name] [:lower s]]
                    [:> [:word_similarity [:lower s] [:lower :name]] similarity-threshold]]
            :order-by [[[:word_similarity [:lower s] [:lower :name]] :desc]]
            :limit limit}
           (query db-access ,,,)
           (map #(transform-row group-spec %))
           (map map->Group ,,,))))

  (update-group [_ group]
    (query db-access {:update [:groups]
                      :set group
                      :where [:= :id (:id group)]}))

  (add-to-group [_ group-id member-id role]
    (query db-access {:insert-into [:group-memberships]
                      :columns [:group_id :member_id :role]
                      :values [[group-id member-id role]]}))

  (find-groups-by-member [_ member-id]
    (->> {:select [:groups.*]
          :from [:groups]
          :join [:group_memberships [:= :groups.id :group_memberships.group_id]]
          :where [:= :group_memberships.member_id member-id]}
         (query db-access ,,,)
         (map #(transform-row group-spec %) ,,,)
         (map map->Group)))

  ; TODO Implement Member domain entity and return members (not users)
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
    (query db-access {:select [:*] :from [:groups]})
    (->> {:select [:*] :from [:groups]}
         (query db-access ,,,)
         (map #(transform-row group-spec %) ,,,)
         (map map->Group ,,,)))

  (find-membership-by-member [_ group-id member-id]
    (->> {:select [:group_memberships.*]
          :from [:group_memberships]
          :where [:and [:= :group_id group-id] [:= :member_id member-id]]}
         (query db-access ,,,)
         (first ,,,)
         (transform-row group-membership-spec ,,,)
         (map->GroupMembership ,,,)))

  (delete-group [_ group-id]
    (query db-access {:delete-from [:groups]
                      :where [:= :id group-id]})))

(def last-db (atom nil))

(defn new-group-repository [db-access]
  (reset! last-db (DbGroupRepository. db-access))
  (DbGroupRepository. db-access))

(comment
  (gr/find-group-by-id @last-db (parse-uuid "0199020b-2203-70a6-a6fe-9975337fa679"))
  (gr/find-membership-by-member @last-db (parse-uuid "0199020b-2203-70a6-a6fe-9975337fa679") (parse-uuid "4f62d3a0-0690-496a-9c43-a3f2561e51f6")))

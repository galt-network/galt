(ns galt.members.adapters.db-member-repository
  (:require
    [galt.core.adapters.db-access :as db :refer [query query-one]]
    [galt.members.domain.entities.member :as member]
    [honey.sql.helpers :refer [where limit]]
    [galt.core.adapters.db-result-transformations :refer [transform-row
                                                          defaults
                                                          ->local-date-time
                                                          map-without-nils]]
    [galt.members.domain.member-repository :as mr :refer [MemberRepository]]))

(def member-spec
  {:members/id defaults
   :members/name defaults
   :members/description defaults
   :members/slug defaults
   :members/avatar defaults
   :members/user_id defaults
   :members/location_id defaults
   :members/created_at [(first defaults) ->local-date-time]})

(defrecord DbMemberRepository [db-access]
  MemberRepository

  (add-member [_ member]
    (->> {:insert-into [:members] :values [(map-without-nils member)] :returning [:*]}
         (query-one db-access ,,,)
         (transform-row member-spec ,,,)
         (member/map->Member ,,,)))

  (update-member [_ member-id attributes]
    (->> {:update [:members]
          :set (map-without-nils attributes)
          :where [:= :id member-id]
          :returning [:*]}
         (query-one db-access ,,,)
         (transform-row member-spec ,,,)
         (member/map->Member ,,,)))

  (find-members-by-name [_ s]
    (mr/find-members-by-name _ s nil))

  (find-members-by-name [_ s group-id]
    (some->> {:select-distinct-on [[:members.id] :members.*]
              :from [:members]
              :left-join [:group-memberships [:= :group_memberships.member_id :members.id]]
              :where [:and
                      (if (nil? group-id) [:= 1 1] [:= :group_memberships.group_id group-id])
                      [:ilike :name (str "%" s "%")]]
              :limit 10}
             (query db-access ,,,)
             (map #(transform-row member-spec %) ,,,)
             (map member/map->Member ,,,)))

  (find-member-by-id [_ id]
    (some->> {:select [:*] :from [:members] :where [:= :id id]}
             (query-one db-access ,,,)
             (transform-row member-spec ,,,)
             (member/map->Member ,,,)))

  (list-members [_]
    (mr/list-members _ {:limit 10}))

  (list-members [_ {:keys [query group-id limit order offset] :or {limit 10 offset 0 order :name}}]
    (let [inner-query (cond-> {:select-distinct-on [[:members.id] :members.*]
                               :from [:members]
                               :left-join [[:group-memberships :gm] [:= :gm.member_id :members.id]]
                               :order-by [:members.id]
                               :limit limit
                               ; :where [:and [:= 1 1]]
                               :offset offset}
                        (seq query) (where [:ilike :name (str "%" query "%")])
                        group-id (where [:= :gm.group_id group-id]))]
      (some->>
        {:select [:*] :from inner-query :order-by order}
        (db/query db-access ,,,)
        (map #(transform-row member-spec %) ,,,)
        (map member/map->Member ,,,))))

  (find-member-by-user-id [_ id]
    (some->> {:select [:*] :from [:members] :where [:= :id id]}
             (query-one db-access ,,,)
             (transform-row member-spec ,,,)
             (member/map->Member ,,,)))

  (fuzzy-find-member [_ s]
    (mr/fuzzy-find-member _ s nil))

  (fuzzy-find-member [_ s group-id]
    (let [limit 10
          similarity-threshold 0.3]
      (->> {:select [:members.*]
            :from [:members]
            :where [:or
                    [:% [:lower :name] [:lower s]]
                    [:> [:word_similarity [:lower s] [:lower :name]] similarity-threshold]]
            :order-by [[[:word_similarity [:lower s] [:lower :name]] :desc]]
            :limit limit}
           (query db-access ,,,)
           (map #(transform-row member-spec %))
           (map member/map->Member ,,,)))))

(def last-repo (atom nil))
(defn new-db-member-repository [db-access]
  (reset! last-repo (DbMemberRepository. db-access))
  (DbMemberRepository. db-access))

(comment
  (mr/fuzzy-find-member @last-repo "G")
  (mr/list-members @last-repo)
  (member/map->Member nil))

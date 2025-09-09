(ns galt.members.adapters.db-member-repository
  (:require
    [galt.core.adapters.db-access :refer [query query-one]]
    [galt.members.domain.entities.member :as member]
    [galt.core.adapters.db-result-transformations :refer [transform-row defaults ->local-date-time]]
    [galt.members.domain.member-repository :as mr :refer [MemberRepository]]))

(def member-spec
  {:members/id defaults
   :members/name defaults
   :members/slug defaults
   :members/avatar defaults
   :members/user_id defaults
   :members/created_at [(first defaults) ->local-date-time]})

(defrecord DbMemberRepository [db-access]
  MemberRepository

  (add-member [_ member])

  (find-member-by-id [_ id]
    (some->> {:select [:*] :from [:members] :where [:= :id id]}
             (query-one db-access ,,,)
             (transform-row member-spec ,,,)
             (member/map->Member ,,,)))

  (find-member-by-user-id [_ id]
    (some->> {:select [:*] :from [:members] :where [:= :user_id id]}
             (query-one db-access ,,,)
             (transform-row member-spec ,,,)
             (member/map->Member ,,,)))

  (fuzzy-find-member [_ s]
    (let [limit 10
          similarity-threshold 0.2]
      (->> {:select [:*]
            :from [:users]
            :where [:or
                    [:% [:lower :name] [:lower s]]
                    [:> [:word_similarity [:lower s] [:lower :name]] similarity-threshold]]
            :order-by [[[:word_similarity [:lower s] [:lower :name]] :desc]]
            :limit limit}
           (query db-access ,,,)
           (map #(transform-row member-spec %))
           (map member/map->Member ,,,)))))

(defn new-db-member-repository [db-access]
  (DbMemberRepository. db-access))

(comment
  (member/map->Member nil))

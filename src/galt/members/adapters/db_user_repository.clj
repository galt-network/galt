(ns galt.members.adapters.db-user-repository
  (:require
    [galt.core.adapters.db-access :refer [query]]
    [galt.members.domain.user-repository :refer [UserRepository]]))

(defn active-user->user
  [user]
  {:users/id (:active_users/id user)
   :users/pub-key (:active_users/pub_key user)
   :users/name (:active_users/name user)
   :users/created-at (:active_users/created_at user)})

(defrecord DbUserRepository [db-access]
  UserRepository

  (add-user [_ name id pub-key]
    (query db-access
           {:insert-into [:users]
            :columns [:name :id :pub-key]
            :values [[name id pub-key]]
            :returning [:*]}))

  (delete-user [_ id]
    (query db-access
           {:update [:users]
            :set {:deleted_at [:raw "NOW()"]}
            :where [:= :users/id id]}))

  (find-user-by-id [_ id]
    (->> {:select [:*] :from [[:active_users :users]] :where [:= :id id] :limit 1}
         (query db-access ,,,)
         (map active-user->user ,,,)
         first))

  (find-user-by-pub-key [_ pub-key]
    (->> {:select [:*] :from [:active_users] :where [:= :pub_key pub-key]}
         (query db-access ,,,)
         (map active-user->user ,,,)
         first))

  (list-users [_]
    (->> {:select [:*] :from [[:active_users :users]]}
         (query db-access ,,,)
         (map active-user->user ,,,))))

(defn new-db-user-repository [db-access]
  (DbUserRepository. db-access))

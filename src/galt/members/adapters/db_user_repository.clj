(ns galt.members.adapters.db-user-repository
  (:require
    [galt.core.adapters.db-access :refer [query query-one]]
    [galt.members.domain.entities.user :as user]
    [galt.core.adapters.db-result-transformations :refer [transform-row defaults ->local-date-time]]
    [galt.core.infrastructure.name-generator :as name-generator]
    [galt.members.domain.user-repository :as ur :refer [UserRepository]]))

(def user-spec
  {:users/id defaults
   :users/pub_key defaults
   :users/created_at [(first defaults) ->local-date-time]})

(defn add-name-generated-from-pub-key
  [user]
  (assoc user :name (name-generator/generate (:pub-key user))))

(defrecord DbUserRepository [db-access]
  UserRepository

  (add-user [_ id pub-key]
    (->> {:insert-into [:users] :columns [:id :pub-key] :values [[id pub-key]] :returning [:*]}
         (query db-access ,,,)
         (first ,,,)
         (transform-row user-spec ,,,)
         (user/map->User ,,,)))

  (delete-user [_ id]
    (query db-access
           {:update [:users]
            :set {:deleted_at [:raw "NOW()"]}
            :where [:= :users/id id]}))

  (find-user-by-id [_ id]
    (some->> {:select [:*] :from [:users] :where [:= :id id] :limit 1}
             (query-one db-access ,,,)
             (transform-row user-spec ,,,)
             (add-name-generated-from-pub-key ,,,)))

  (find-user-by-pub-key [_ pub-key]
    (some->> {:select [:*] :from [:users] :where [:= :pub_key pub-key]}
             (query-one db-access ,,,)
             (transform-row user-spec)
             (add-name-generated-from-pub-key ,,,)))

  (list-users [_]
    (->> {:select [:*] :from [:users]}
         (query db-access ,,,)
         (map #(transform-row user-spec %) ,,,)
         (map add-name-generated-from-pub-key ,,,))))

(defn new-db-user-repository [db-access]
  (DbUserRepository. db-access))

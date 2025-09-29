(ns galt.posts.adapters.db-post-repository
  (:require
    [galt.core.adapters.db-access :refer [query query-one]]
    [galt.core.adapters.db-result-transformations :as db-transform :refer [transform-row
                                                                           defaults]]
    [galt.posts.domain.post-repository :as pr :refer [PostRepository]]))

(def post-spec
  {:posts/id defaults
   :posts/title defaults
   :posts/content defaults
   :members/author defaults
   :members/author_avatar defaults
   :members/author_id defaults
   :posts/target_type defaults
   :posts/target_id defaults
   :posts/comment_policy defaults
   :posts/hidden defaults
   :posts/publish_at db-transform/default-datetime
   :posts/created_at db-transform/default-datetime})

(defrecord DbPostRepository [db-access]
  PostRepository
  (add-post [_ post]
    (->> {:insert-into [:posts] :values [post]}
         (query-one db-access ,,,)))

  (group-posts [_ group-id]
    (->> {:select [:posts.*
                   [:members.name :author]
                   [:members.avatar :author-avatar]
                   [:members.id :author-id]
                   [:members.slug :author-slug]]
          :from [:posts]
          :join [:members [:= :members.id :posts.author-id]]
          :where [:and
                  [:= :target-type "group"]
                  [:in :target-id (if (coll? group-id) group-id [group-id])]]}
         (query db-access ,,,)
         (map #(transform-row post-spec %) ,,,)
         )))

(def last-repo (atom nil))

(defn new-db-post-repository [db-access]
  (reset! last-repo (DbPostRepository. db-access))
  (DbPostRepository. db-access))

(comment
  (pr/group-posts @last-repo (parse-uuid "01997bba-dbe1-7067-9135-1a300d70e218"))
  )

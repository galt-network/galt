(ns galt.posts.adapters.db-post-repository
  (:require
   [galt.core.adapters.db-access :refer [query query-one]]
   [galt.core.adapters.db-result-transformations :as db-transform :refer [defaults
                                                                          transform-row]]
   [galt.posts.domain.post-repository :as pr :refer [PostRepository]]
   [honey.sql.helpers :refer [where]]))

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

  (get-post [_ post-id]
    (some->> {:select [:*] :from [:posts] :where [:= :id post-id]}
             (query-one db-access ,,,)
             (transform-row post-spec ,,,)))

  (list-posts [_ {:keys [group-id limit offset] :or {limit 10 offset 0}}]
    (let [base-q {:select [:posts.*
                          [:members.name :author]
                          [:members.avatar :author-avatar]
                          [:members.id :author-id]
                          [:members.slug :author-slug]]
                 :from [:posts]
                 :join [:members [:= :members.id :posts.author-id]]
                 :where [:and [:= 1 1]]
                 :limit limit
                 :offset offset
                 :order-by [:created-at]
                 }
          final-q (cond-> base-q
                    group-id (where [:and
                                     [:= :target-type "group"]
                                     [:in :target-id (if (coll? group-id) group-id [group-id])]]))]
      (->> (query db-access final-q)
           (map #(transform-row post-spec %) ,,,)))))

(def last-repo (atom nil))

(defn new-db-post-repository [db-access]
  (reset! last-repo (DbPostRepository. db-access))
  (DbPostRepository. db-access))

(comment
  (pr/list-posts @last-repo (parse-uuid "01997bba-dbe1-7067-9135-1a300d70e218"))
  )

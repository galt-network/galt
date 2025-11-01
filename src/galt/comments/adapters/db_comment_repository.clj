(ns galt.comments.adapters.db-comment-repository
  (:require
    [galt.core.adapters.db-access :refer [query query-one]]
    [honey.sql.helpers :as sql-helpers :refer [where]]
    [galt.core.adapters.db-result-transformations :as db-transform :refer [transform-row defaults]]
    [galt.comments.domain.comment-repository :as cr :refer [CommentRepository]]))

(def comment-spec
  {:id defaults
   :content defaults
   :parent_id defaults
   :author_id defaults
   :created_at db-transform/default-datetime
   :level defaults
   :members/author_name defaults
   :members/author_avatar defaults})

(defn entity-type->table-info
  [entity-type]
  (case entity-type
    :events [:event-comments :event-id]
    :posts [:post-comments :post-id]))

(defrecord DbEventRepository [db-access]
  CommentRepository

  (add-comment [_ entity-id entity-type {:keys [parent-id content author-id]}]
    (let [added-comment (query-one db-access
                                   {:insert-into [:comments]
                                    :values [{:parent-id parent-id
                                              :content content
                                              :author-id author-id}]
                                    :returning [:*]})
          [table-name fk-column] (entity-type->table-info entity-type)]
      (query db-access
             {:insert-into [table-name]
              :values [{fk-column entity-id
                        :comment-id (:comments/id added-comment)}]})
      added-comment))

  (get-comment [_ comment-id])

  (delete-comment [_ comment-id])

  (list-comments [_ entity-id entity-type {:keys [comment-id limit offset] :or {limit 10 offset 0}}]
    (let [[table-name fk-column] (entity-type->table-info entity-type)
          table-fk-column (keyword (str (name table-name) "." (name fk-column)))
          table-comment-column (keyword (str (name table-name) ".comment-id"))
          base-q {:with-recursive
                  [[:comments-hierarchy
                    {:union-all
                     [{:select [:comments.*, [:comments.id :comment_id] [0 :level]]
                       :from [:comments]
                       :join [table-name [:= table-comment-column :comments.id]]
                       :where [:and
                               [:= :comments.parent_id nil]
                               [:= table-fk-column entity-id]]}
                      {:select [:comments.* [:comments.id :comment_id] [[:+ :comments-hierarchy.level 1] :level]]
                       :from [:comments]
                       :join [table-name [:= table-comment-column :comments.id]
                              :comments-hierarchy [:= :comments.parent_id :comments-hierarchy.id]]
                       :where [:= table-fk-column entity-id]}]}]]
                  :select [:comments-hierarchy.* [:members.name :author_name] [:members.avatar :author_avatar]]
                  :from [:comments-hierarchy]
                  :join [:members [:= :members.id :comments-hierarchy.author-id]]
                  :order-by [:created-at :level]}
          final-q (cond-> base-q
                     comment-id (where [:= :comment_id comment-id])
                     comment-id (sql-helpers/limit 1)
                     limit (sql-helpers/limit limit)
                     offset (sql-helpers/offset offset))]
      (->> (query db-access final-q)
           (map #(transform-row comment-spec %) ,,,)
           (map (fn [c] (assoc c :commented-entity-type entity-type :commented-entity-id entity-id)) ,,,)))))

(def last-repo (atom nil))
(defn new-db-comment-repository
  [db-access]
  (reset! last-repo (DbEventRepository. db-access))
  (DbEventRepository. db-access))

(comment
  (cr/list-comments @last-repo (parse-uuid "0199f438-921b-7879-b06e-f4d0a79def76") :posts {:comment-id 14}))

(ns galt.events.adapters.db-event-repository
  (:require
    [galt.core.adapters.db-access :refer [query query-one]]
    [honey.sql.helpers :refer [where]]
    [honey.sql :as sql]
    [galt.core.adapters.db-result-transformations :as db-transform :refer [transform-row defaults]]
    [galt.events.domain.event-repository :as er :refer [EventRepository]]))

(def event-spec
  {:events/id defaults
   :events/name defaults
   :events/description defaults
   :events/author_id defaults
   :members/author defaults
   :members/author_avatar defaults
   :events/group_id defaults
   :events/location_id defaults
   :events/comment_policy defaults
   :events/type defaults
   :events/hidden defaults
   :events/start_time db-transform/default-datetime
   :events/end_time db-transform/default-datetime
   :events/publish_at db-transform/default-datetime
   :events/created_at db-transform/default-datetime})

(def comment-spec
  {:id defaults
   :content defaults
   :parent_id defaults
   :author_id defaults
   :created_at db-transform/default-datetime
   :level defaults
   :members/author_name defaults
   :members/author_avatar defaults})

(defrecord DbEventRepository [db-access]
  EventRepository

  (add-event [_ event]
    (->> {:insert-into [:events] :values [event]}
         (query-one db-access)))

  (get-event [_ event-id]
    (some->> {:select [:*] :from [:events] :where [:= :id event-id]}
             (query-one db-access ,,,)
             (transform-row event-spec ,,,)))

  (get-comment [_ comment-id]
    (some->> {:select [:comments.* [:members.name :author_name] [:members.avatar :author_avatar]]
              :from [:comments]
              :join [:members [:= :members.id :comments.author-id]]
              :where [:= :comments.id comment-id]}
             (query-one db-access ,,,)
             (transform-row comment-spec ,,,)
             ))

  (update-event [_ event])

  (comment-event [_ event-id {:keys [parent-id content author-id]}]
    (let [added-comment (query-one db-access {:insert-into [:comments]
                                              :values [{:parent-id parent-id
                                                        :content content
                                                        :author-id author-id}]
                                              :returning [:*]})]
      (query db-access
             {:insert-into [:event_comments]
              :values [{:event-id event-id
                        :comment-id (:comments/id added-comment)}]})
      added-comment))

  (list-event-comments [_ event-id params]
    (->> {:with-recursive
          [[:comments-hierarchy
            {:union-all
             [{:select [:comments.*, [0 :level]]
               :from [:comments]
               :join [:event_comments [:= :event_comments.comment_id :comments.id]]
               :where [:and
                       [:= :comments.parent_id nil]
                       [:= :event_comments.event_id event-id]]}
              {:select [:comments.* [[:+ :comments-hierarchy.level 1] :level]]
               :from [:comments]
               :join [:event_comments [:= :event_comments.comment_id :comments.id]
                      :comments-hierarchy [:= :comments.parent_id :comments-hierarchy.id]]
               :where [:= :event_comments.event_id event-id]}]}]]
          :select [:comments-hierarchy.* [:members.name :author_name] [:members.avatar :author_avatar]]
          :from [:comments-hierarchy]
          :join [:members [:= :members.id :comments-hierarchy.author-id]]
          :order-by [:created-at :level]}
         (query db-access ,,,)
         (map #(transform-row comment-spec %) ,,,)))

  (rsvp-event [_ event-id member-id])

  (list-events [_ {:keys [group-id author-id from-date to-date offset limit type]
                      :or {limit 10 offset 0}}]
    (let [base-query {:select [:events.*
                              [:members.name :author]
                              [:members.avatar :author-avatar]
                              [:members.id :author-id]
                              [:members.slug :author-slug]]
                     :from [:events]
                     :join [:members [:= :members.id :events.author-id]]
                     :where [:and [:= 1 1]]
                     :order-by [:events.start-time]
                     :limit limit
                     :offset offset}
          final-query (cond-> base-query
                        type (where [:= :events.type type])
                         group-id (where [:= :group-id group-id])
                         author-id (where [:= :events.author-id author-id])
                         from-date (where [:>= :events.start-time from-date])
                         to-date (where [:<= :events.start-time to-date]))]
      (->> (query db-access final-query)
           (map #(transform-row event-spec %) ,,,)))))

(def dba (atom nil))
(defn new-db-event-repository
  [db-access]
  (reset! dba db-access)
  (DbEventRepository. db-access))

(comment
  (er/list-event-comments (DbEventRepository. @dba) (parse-uuid "0199a040-8695-7013-96d1-ce24b17e46fd") {})
  (require '[galt.core.adapters.time-helpers :as th])
  (map :name (er/list-events (DbEventRepository. @dba) {:limit 20
                                                        :from-date (first (th/period-range :this-week))
                                                        :to-date (second (th/period-range :this-week))}))
  )

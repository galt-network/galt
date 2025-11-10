(ns galt.events.adapters.db-event-repository
  (:require
    [galt.core.adapters.db-access :refer [query query-one]]
    [honey.sql.helpers :refer [where limit]]
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

(defrecord DbEventRepository [db-access]
  EventRepository

  (add-event [_ event]
    (->> {:insert-into [:events] :values [event]}
         (query-one db-access)))

  (get-event [_ event-id]
    (some->> {:select [:*] :from [:events] :where [:= :id event-id]}
             (query-one db-access ,,,)
             (transform-row event-spec ,,,)))

  (update-event [_ event-id attrs])

  (delete-event [_ event-id])

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
  (require '[galt.core.adapters.time-helpers :as th])
  (map :name (er/list-events (DbEventRepository. @dba) {:limit 20
                                                        :from-date (first (th/period-range :this-week))
                                                        :to-date (second (th/period-range :this-week))}))
  )

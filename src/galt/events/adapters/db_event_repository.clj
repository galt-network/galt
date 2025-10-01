(ns galt.events.adapters.db-event-repository
  (:require
    [galt.core.adapters.db-access :refer [query query-one]]
    [honey.sql.helpers :refer [where]]
    [galt.core.adapters.db-result-transformations
     :as db-transform
     :refer [transform-row
             defaults
             ->local-date-time
             map-without-nils]]
    [galt.events.domain.event-repository :as mr :refer [EventRepository]]))

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

  (update-event [_ event])
  (comment-event [_ event-comment])
  (rsvp-event [_ event-rsvp])

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

(defn new-db-event-repository
  [db-access]
  (DbEventRepository. db-access))

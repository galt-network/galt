(ns galt.events.adapters.db-event-repository
  (:require
    [galt.core.adapters.db-access :refer [query query-one]]
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
   :events/publish_at db-transform/default-datetime
   :events/created_at db-transform/default-datetime})

(defrecord DbEventRepository [db-access]
  EventRepository

  (add-event [this event]
    (->> {:insert-into [:events] :values [event]}
         (query-one db-access)))

  (update-event [this event])
  (comment-event [this event-comment])
  (rsvp-event [this event-rsvp])

  (list-events [this {:keys [group-id author-id from-date to-date]}]
    (->> {:select [:events.*
                   [:members.name :author]
                   [:members.avatar :author-avatar]
                   [:members.id :author-id]
                   [:members.slug :author-slug]]
          :from [:events]
          :join [:members [:= :members.id :events.author-id]]
          :where [:and
                  [:= 1 1]
                  (cond
                    group-id [:= :group-id group-id]
                    author-id [:= :events.author-id author-id]
                    from-date [:> :events.])]}
         (query db-access ,,,)
         (map #(transform-row event-spec %) ,,,))))

(defn new-db-event-repository
  [db-access]
  (DbEventRepository. db-access))

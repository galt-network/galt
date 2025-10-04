(ns galt.events.domain.event-repository)

(defprotocol EventRepository
  (add-event [this event])
  (get-event [this event-id])
  (get-comment [this comment-id])
  (update-event [this event])
  (comment-event [this event-id comment])
  (rsvp-event [this event-id member-id])
  (list-events [this params])
  (list-event-comments [this event-id params]))

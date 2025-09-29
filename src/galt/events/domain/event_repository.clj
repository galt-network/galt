(ns galt.events.domain.event-repository)

(defprotocol EventRepository
  (add-event [this event])
  (update-event [this event])
  (comment-event [this event-comment])
  (rsvp-event [this event-rsvp])
  (list-events [this params]))

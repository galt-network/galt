(ns galt.events.domain.event-repository)

(defprotocol EventRepository
  (add-event [this event])
  (get-event [this event-id])
  (update-event [this event-id attrs])
  (delete-event [this event-id])
  (rsvp-event [this event-id member-id])
  (list-events [this params]))

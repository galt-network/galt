(ns galt.events.domain.use-cases.add-event)

; TODO Checks that this user isn't spamming
;   < 10 events created within past hour
;   < 10 events created within past month
;   check against user settings (can override defaults)
(defn add-event-use-case
  [{:keys [add-event]}
   {:keys [event]}]
  [:ok (add-event event)])

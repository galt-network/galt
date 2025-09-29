(ns galt.events.external.routes
  (:require
    [reitit.ring :as rr]
    [galt.events.adapters.handlers :as events]))

(defn router
  [deps]
  (let [with-layout (:with-layout deps)
        with-deps-layout (partial with-layout deps)]
    (rr/router
      [["/events/new" {:id :events
                       :name :events/new
                       :get (with-deps-layout events/new-event)}]
       ["/events" {:id :events
                       :name :events
                       :post (with-deps-layout events/create)
                       :get (with-deps-layout events/list-events)}]])))

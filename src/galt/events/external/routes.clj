(ns galt.events.external.routes
  (:require
    [reitit.ring :as rr]
    [galt.events.adapters.handlers :as events]))

(defn router
  [deps]
  (let [with-layout (:with-layout deps)
        with-deps-layout (partial with-layout deps)]
    (rr/router
      [["/events" {:id :events
                   :name :events
                   :post (with-deps-layout events/create)
                   :get (with-deps-layout events/list-events)}]
       ["/events/new" {:id :events
                       :conflicting true
                       :name :events/new
                       :get (with-deps-layout events/new-event)}]
       ["/events/:id" {:id :events
                       :conflicting true
                       :name :events/by-id
                       :get (with-deps-layout events/show-event)}]
       ["/events/:id/comments" {:id :events
                                :name :events.by-id/comments
                                :get (partial events/show-comment-form deps)
                                :post (partial events/send-comment deps)}]])))

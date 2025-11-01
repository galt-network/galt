(ns galt.comments.external.routes
  (:require
   [galt.comments.adapters.comment-handlers :as handlers]
   [reitit.coercion.spec]
   [reitit.ring :as rr]))

(defn router
  [deps]
  (let [with-layout (:with-layout deps)
        with-deps-layout (partial with-layout deps)]
    (rr/router
      [["/:entity-type/:entity-id/comments"
        {:id :comments
         :name :comments
         :conflicting true
         :get (partial handlers/show-comments deps)
         :post (partial handlers/send-comment deps)}]])))

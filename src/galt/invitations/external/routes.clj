(ns galt.invitations.external.routes
  (:require
    [reitit.ring :as rr]
    [galt.invitations.adapters.handlers :as handlers]))

(defn router
  [deps]
  (let [with-layout (:with-layout deps)
        with-deps-layout (partial with-layout deps)]
    (rr/router
      [["/invitations/new" {:name :invitations/new
                            :get (with-deps-layout handlers/new-invitation)}]]
      {:reitit.middleware/registry (:reitit-middleware deps)})))

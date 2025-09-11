(ns galt.invitations.external.routes
  (:require
    [reitit.ring :as rr]
    [galt.invitations.adapters.handlers :as handlers]))

(defn router
  [deps]
  (let [with-layout (:with-layout deps)
        with-deps-layout (partial with-layout deps)]
    (rr/router
      [["/invitations/new-request" {:name :invitations/new-request
                                    :get (with-deps-layout handlers/new-invitation-request)
                                    :post (with-deps-layout handlers/create-invitation-request)}]
       ["/invitations" {:id :invitations
                        :name :invitations
                        :get (with-deps-layout handlers/list-invitations)}]]
      {:reitit.middleware/registry (:reitit-middleware deps)})))

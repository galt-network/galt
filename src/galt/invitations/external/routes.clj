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
       ["/invitations/new" {:name :invitations/new
                            :get (with-deps-layout handlers/new-invitation)
                            :post (with-deps-layout handlers/create-invitation)}]
       ["/invitations" {:id :invitations
                        :name :invitations
                        :get (with-deps-layout handlers/list-invitations)}]
       ["/i/:id" {:name :invitations/by-id
                  :get (with-deps-layout handlers/show-invitation)}]])))

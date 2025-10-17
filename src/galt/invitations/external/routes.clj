(ns galt.invitations.external.routes
  (:require
    [reitit.ring :as rr]
    [galt.invitations.adapters.handlers :as handlers]
    [galt.invitations.adapters.steps-handlers :as steps]
    ))

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
       ["/invitations/search-group" {:name :invitations/search-groups
                                     :conflicting true
                                     :get (partial handlers/search-groups deps)}]
       ["/invitations" {:id :invitations
                        :min-role :member
                        :name :invitations
                        :get (with-deps-layout handlers/list-invitations)}]
       ["/i/:id"
        ["" {:name :invitations/by-id :get (with-deps-layout handlers/show-invitation)}]
        ["/steps"
         ["" {:name :invitations.steps :get (with-deps-layout steps/steps)}]
         ["/1-start" {:name :invitations.steps/start :get (with-deps-layout steps/show-start)}]
         ["/2-login" {:name :invitations.steps/login :get (with-deps-layout steps/show-login)}]
         ["/2-login/status" {:name :invitations.steps/login-status :get (with-deps-layout steps/login-status)}]
         ["/2-login/callback" {:name :invitations.steps/lnurl-callback
                               :get (with-deps-layout steps/lnurl-callback)}]
         ["/3-payment" {:name :invitations.steps/payment :get (with-deps-layout steps/show-payment)}]
         ["/4-complete" {:name :invitations.steps/complete :get (with-deps-layout steps/show-complete)}]]]])))

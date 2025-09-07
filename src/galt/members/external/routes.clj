(ns galt.members.external.routes
  (:require
    [galt.core.infrastructure.web.helpers :refer [->json]]
    [galt.core.infrastructure.bitcoin.lnurl :as lnurl]
    [galt.core.infrastructure.bitcoin.bouncy-castle-verify :refer [verify-signature]]
    [reitit.ring :as rr]
    [galt.members.adapters.handlers :as members]
    ))

(defn router
  [deps]
  (let [with-layout (:with-layout deps)
        with-deps-layout (partial with-layout deps)]
    (rr/router
      [["/members" {:id :members
                    :get (with-deps-layout members/show-members-list)}]
       ["/members/me" {:id :profile
                       :conflicting true
                       :get (with-deps-layout members/show-my-profile)
                       :middleware [:auth]}]
       ["/members/me/edit" {:id :members
                            :get (with-deps-layout members/edit-my-profile)}]
       ["/members/login" {:id :login
                          :conflicting true
                          :get (with-deps-layout members/show-login)
                          :post (with-layout
                                  (merge deps
                                         {:generate-lnurl (partial lnurl/generate-lnurl
                                                                   (str (:galt-url deps)
                                                                        "/members/login/lnurl-auth"))})
                                  members/do-login)}]
       ["/members/logout" {:id :login
                           :conflicting true
                           :post (with-deps-layout members/logout)}]
       ["/members/login/lnurl-auth" {:id :lnurl-auth
                                     :get (with-layout
                                            (-> deps
                                                (assoc ,,, :->json ->json)
                                                (assoc ,,, :verify-signature verify-signature))
                                            members/lnurl-auth-callback)}]
       ["/members/:id" {:id :members
                        :name :members/show-profile
                        :conflicting true
                       :get (with-deps-layout members/show-profile)}]]
      {:reitit.middleware/registry (:reitit-middleware deps)})))

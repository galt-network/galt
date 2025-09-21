(ns galt.members.external.routes
  (:require
   [galt.core.infrastructure.bitcoin.lnurl :as lnurl]
   [galt.core.infrastructure.web.helpers :refer [->json]]
   [galt.members.adapters.handlers :as members]
   [galt.members.adapters.login-handlers :as login-handlers]
   [reitit.ring :as rr]))

(defn router
  [deps]
  (let [with-layout (:with-layout deps)
        with-deps-layout (partial with-layout deps)]
    (rr/router
      [["/members" {:id :members
                    :get (with-deps-layout members/show-members-list)}]
       ["/members/new" {:id :members
                        :name :members/new
                        :conflicting true
                        :get (with-deps-layout members/new-member)}]
       ["/members/search" {:id :members/search
                           :conflicting true
                           :get (with-deps-layout members/search-members)}]
       ["/members/me" {:id :members
                       :name :members/me
                       :conflicting true
                       :get (with-deps-layout members/show-my-profile)
                       :min-role :user}]
       ["/members/me/edit" {:id :members
                            :get (with-deps-layout members/edit-my-profile)}]
       ["/members/login" {:id :login
                          :conflicting true
                          :get (with-deps-layout login-handlers/show-login)
                          :post (with-layout
                                  (merge deps
                                         {:generate-lnurl (partial lnurl/generate-lnurl
                                                                   (:galt-url deps)
                                                                   "/members/login/lnurl-auth")})
                                  login-handlers/do-login)}]
       ["/members/logout" {:id :login
                           :conflicting true
                           :post (with-deps-layout login-handlers/logout)}]
       ["/members/login/lnurl-auth" {:id :lnurl-auth
                                     :get (with-layout
                                            (-> deps (assoc ,,, :->json ->json))
                                            login-handlers/lnurl-auth-callback)}]
       ["/members/:id" {:id :members
                        :name :members/by-id
                        :conflicting true
                       :get (with-deps-layout members/show-profile)}]])))

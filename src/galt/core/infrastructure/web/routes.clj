(ns galt.core.infrastructure.web.routes
  (:require
    [galt.core.infrastructure.web.helpers :refer [->json render-html with-layout]]
    [galt.core.infrastructure.bitcoin.lnurl :as lnurl]
    [galt.core.infrastructure.web.middleware :as middleware]
    [galt.core.infrastructure.bitcoin.bouncy-castle-verify :refer [verify-signature]]
    [galt.core.adapters.handlers :as core-handlers]
    [galt.core.views.layout :as layout]
    [galt.groups.adapters.handlers :as groups]
    [galt.members.adapters.handlers :as members]
    [reitit.ring :as rr]
    [ring.middleware.cors :refer [wrap-cors]]
    [ring.middleware.content-type :refer [wrap-content-type]]
    [ring.middleware.session :refer [wrap-session]]
    [ring.middleware.keyword-params :refer [wrap-keyword-params]]
    [ring.middleware.params :refer [wrap-params]]
    [ring.middleware.multipart-params :refer [wrap-multipart-params]]
    [ring.middleware.session.memory :as memory]
    ))

(defonce galt-session-atom (atom {}))

(defn router
 [deps]
 (let [base-deps {:app-container layout/app-container
                  :render render-html
                  :galt-session galt-session-atom}
       route-deps (merge base-deps deps)
       with-deps-layout (partial with-layout route-deps)]
   (rr/router
     [["/" {:id :home
            :get (with-deps-layout core-handlers/view-landing)}]
      ["/groups" {:id :groups
                  :get (with-deps-layout groups/list-groups)
                  :post {:handler (with-deps-layout groups/create-group)
                         :middleware [:auth]}}]
      ["/groups/new" {:id :groups
                      :conflicting true
                      :get (with-deps-layout groups/new-group)
                      :middleware [:auth]}]
      ["/groups/:id" {:id :groups
                      :name :groups/show-group
                      :conflicting true
                      :get (with-deps-layout groups/show-group)
                      :put (with-deps-layout groups/update-group)}]
      ["/groups/:id/edit" {:id :groups
                           :name :groups/edit-group
                           :get (with-deps-layout groups/edit-group)}]
      ["/members" {:id :members
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
                                 (merge route-deps
                                        {:generate-lnurl (partial lnurl/generate-lnurl
                                                                  (str (:galt-url route-deps)
                                                                       "/members/login/lnurl-auth"))})
                                 members/do-login)}]
      ["/members/logout" {:id :login
                          :conflicting true
                          :post (with-deps-layout members/logout)}]
      ["/members/login/lnurl-auth" {:id :lnurl-auth
                                    :get (with-layout
                                           (-> route-deps
                                               (assoc ,,, :->json ->json)
                                               (assoc ,,, :verify-signature verify-signature))
                                           members/lnurl-auth-callback)}]
      ["/members/:id" {:id :members
                       :name :members/show-profile
                       :conflicting true
                       :get (with-deps-layout members/show-profile)}]
      ["/files" {:post (partial core-handlers/store-file deps)}]
      ["/files/*path" {:get (partial core-handlers/serve-file deps)}]
      ["/assets/*" (-> (rr/create-resource-handler)
                       (wrap-cors ,,,
                                  :access-control-allow-origin #".*"
                                  :access-control-allow-methods [:get])
                       (wrap-content-type ,,, {:mime-types {"cljs" "application/x-scittle"}}))]]
     {:reitit.middleware/registry (:reitit-middleware deps)})))

(defn handler [router]
  (-> (rr/ring-handler router nil {:middleware [{:name :logging
                                                 :description "Telemere reitit/ring logging"
                                                 :wrap middleware/wrap-with-logger}]})
      wrap-multipart-params
      wrap-keyword-params
      wrap-params
      (middleware/wrap-add-galt-session ,,, galt-session-atom)
      (wrap-session ,,, {:store (memory/memory-store galt-session-atom)})))

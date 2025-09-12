(ns galt.core.infrastructure.web.middleware-test
  (:require
   [clojure.test :refer [deftest is]]
   [reitit.ring]
   [reitit.core]
   [galt.core.infrastructure.web.middleware :as middleware]))

(deftest wrap-auth-test
  (let [always-ok-handler (constantly {:status 200})
        routes [["/login" {:name :login :get always-ok-handler}]
                ["/groups" {:name :groups
                            :min-role :user
                            :get {:handler always-ok-handler :min-role :user}
                            :post {:handler always-ok-handler :min-role :member}}]
                ["/groups/new" {:name :groups/new :post always-ok-handler :min-role :member}]
                ["/explode" {:name :explode :delete always-ok-handler :min-role :admin}]]
        router (reitit.ring/router routes)
        handler (-> (reitit.ring/ring-handler router nil)
                    (middleware/wrap-auth ,,, router))
        ->req (fn [method uri session]
                {:request-method method :uri uri :session session})]
    (is (= 200 (:status (handler (->req :get "/login" {})))))
    (is (= 302 (:status (handler (->req :get "/groups" {})))))
    (is (= 200 (:status (handler (->req :get "/groups" {:user-id 1})))))
    (is (= 302 (:status (handler (->req :post "/groups" {:user-id 1})))))
    (is (= 200 (:status (handler (->req :post "/groups" {:user-id 1 :member-id 2})))))
    (is (= 302 (:status (handler (->req :post "/groups/new" {:user-id 1})))))
    (is (= 200 (:status (handler (->req :post "/groups/new" {:user-id 1 :member-id 2})))))
    (is (= 302 (:status (handler (->req :delete "/explode" {:user-id 1 :member-id 2})))))
    (is (= 200 (:status (handler (->req :delete "/explode" {:admin true})))))))

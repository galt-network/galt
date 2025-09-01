(ns galt.core.adapters.link-generator-test
  (:require
   [clojure.test :refer [deftest is]]
   [galt.core.adapters.link-generator :refer [link-for-route]]
   [reitit.ring :as rr]))


(deftest generate-url-test
  (let [routes [["/users/:id" {:name :users/view-user}]]
        router (rr/router routes)]
    (is (= "/users/123" (link-for-route router :users/view-user {:id 123})))))

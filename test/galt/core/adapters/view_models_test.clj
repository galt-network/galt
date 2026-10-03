(ns galt.core.adapters.view-models-test
  (:require
    [clojure.test :refer [deftest is]]
    [galt.core.adapters.view-models :as vm]
    [galt.members.domain.member-repository :as mr]
    [galt.members.domain.user-repository :as ur]))

(defn- stub-deps []
  {:user-repo (reify ur/UserRepository
                (find-user-by-id [_ _] nil))
   :member-repo (reify mr/MemberRepository
                  (find-member-by-user-id [_ _] nil))
   :asset-url identity})

(deftest navbar-lists-connect-first
  (let [items (:items (vm/navbar-model (stub-deps) {}))]
    (is (= {:title "Connect" :href "/connect"}
           (select-keys (first items) [:title :href])))))

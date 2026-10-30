(ns galt-promo.arcs-test
  (:require [cljs.test :refer [deftest is testing]]
            [galt-promo.arcs :as arcs]))

(defn- user [id connected? & [location]]
  {:id id :connected? connected? :location location})

(def tallinn {:lat 59.437 :lng 24.7536})
(def helsinki {:lat 60.1699 :lng 24.9384})

(deftest initial-snapshot-is-a-baseline
  (is (= [] (arcs/transition-arcs nil [(user "a" true tallinn)]))))

(deftest online-users-emit-green-arcs
  (testing "a new user arcs to every other locatable user"
    (is (= [{:source-id "b" :target-id "a"
             :startLat 60.1699 :startLng 24.9384
             :endLat 59.437 :endLng 24.7536
             :color arcs/online-color :kind :online}]
           (arcs/transition-arcs
            [(user "a" true tallinn)]
            [(user "a" true tallinn) (user "b" true helsinki)]))))
  (testing "a disconnected user coming back online also emits arcs"
    (is (= [:online]
           (mapv :kind
                 (arcs/transition-arcs
                  [(user "a" true tallinn) (user "b" false helsinki)]
                  [(user "a" true tallinn) (user "b" true helsinki)]))))))

(deftest offline-users-emit-red-arcs
  (is (= [{:source-id "a" :target-id "b" :color arcs/offline-color :kind :offline}]
         (mapv #(select-keys % [:source-id :target-id :color :kind])
               (arcs/transition-arcs
                [(user "a" true tallinn) (user "b" true helsinki)]
                [(user "a" false tallinn) (user "b" true helsinki)])))))

(deftest unchanged-and-removed-users-do-not-emit-arcs
  (is (= [] (arcs/transition-arcs
             [(user "a" true tallinn) (user "b" true helsinki)]
             [(user "a" true tallinn)]))))

(deftest arcs-require-two-known-locations
  (is (= [] (arcs/transition-arcs
             [(user "a" true tallinn)]
             [(user "a" true tallinn) (user "b" true)])))
  (is (= [] (arcs/transition-arcs
             [(user "a" true)]
             [(user "a" true) (user "b" true helsinki)]))))

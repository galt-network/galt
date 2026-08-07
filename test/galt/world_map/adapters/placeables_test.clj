(ns galt.world-map.adapters.placeables-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [galt.world-map.adapters.placeables :as wp]
   [is.galt.globo.protocols :as protocols]
   [is.galt.globo.server.placeables :as placeables]))

(deftest anon-user-id?-test
  (testing "anon prefix matches"
    (is (true? (wp/anon-user-id? "anon-1234"))))
  (testing "member ids do not"
    (is (false? (wp/anon-user-id? "member-1")))
    (is (false? (wp/anon-user-id? nil)))))

(deftest per-user-placeables-test
  (let [provider (wp/per-user-placeables)]
    (testing "anonymous user gets restricted list"
      (let [objects (protocols/placeable-objects provider "anon-xyz")]
        (is (= #{"carrot" "tree" "ancap-flag"}
               (set (map :model-id objects))))))
    (testing "member gets full list"
      (is (= (set (map :model-id placeables/default-config))
             (set (map :model-id (protocols/placeable-objects provider "member-1"))))))
    (testing "custom arity"
      (let [p (wp/per-user-placeables [{:model-id "a"} {:model-id "b"}]
                                      [{:model-id "a"}])]
        (is (= [{:model-id "a"}]
               (protocols/placeable-objects p "anon-1")))
        (is (= [{:model-id "a"} {:model-id "b"}]
               (protocols/placeable-objects p "member-1")))))))

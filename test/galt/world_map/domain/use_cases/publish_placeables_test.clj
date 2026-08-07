(ns galt.world-map.domain.use-cases.publish-placeables-test
  (:require
   [cheshire.core :as json]
   [clojure.test :refer [deftest is testing]]
   [clojure.walk :as walk]
   [galt.world-map.adapters.placeables :as wp]
   [galt.world-map.domain.use-cases.publish-placeables :as uc]
   [is.galt.globo.protocols :as protocols]
   [is.galt.globo.server :as globo]
   [is.galt.globo.server.placeables :as placeables]))

(defn make-globo []
  (let [g (globo/create-globo {:mount-path "/world-map"
                               :placeables (wp/per-user-placeables)})]
    (protocols/update-user! (:storage g) "member-1"
                            #(assoc % :id "member-1" :name "Me" :favorites []))
    (protocols/add-user-connection! (:storage g) "member-1" "conn-1")
    (protocols/add-connection! (:connections g) "conn-1" :channel-1)
    g))

(defn recorded-event [sent]
  (-> (second (first @sent)) (subs 6) json/parse-string walk/keywordize-keys))

(deftest publish-placeables-use-case-test
  (testing "with user-id targets own connections with per-user list"
    (let [g (make-globo)
          sent (atom [])
          result (with-redefs [org.httpkit.server/send! (fn [ch data & _] (swap! sent conj [ch data]) true)]
                   (uc/publish-placeables-use-case {:globo g} {:user-id "member-1"}))]
      (is (= [:ok true] result))
      (is (= 1 (count @sent)))
      (let [event (recorded-event sent)]
        (is (= "placeable-map-objects" (:type event)))
        (is (= (set (map :model-id (protocols/placeable-objects (wp/per-user-placeables) "member-1")))
               (set (map :model-id (:objects (:content event)))))))))
  (testing "anon user gets restricted list"
    (let [g (make-globo)
          _ (protocols/add-user-connection! (:storage g) "anon-1" "conn-2")
          _ (protocols/add-connection! (:connections g) "conn-2" :channel-2)
          sent (atom [])
          result (with-redefs [org.httpkit.server/send! (fn [ch data & _] (swap! sent conj [ch data]) true)]
                   (uc/publish-placeables-use-case {:globo g} {:user-id "anon-1"}))]
      (is (= [:ok true] result))
      (is (= 1 (count @sent)))
      (let [event (recorded-event sent)]
        (is (= #{"carrot" "tree" "ancap-flag"}
               (set (map :model-id (:objects (:content event)))))))))
  (testing "no user-id broadcasts base list to everybody"
    (let [g (make-globo)
          sent (atom [])
          result (with-redefs [org.httpkit.server/send! (fn [ch data & _] (swap! sent conj [ch data]) true)]
                   (uc/publish-placeables-use-case {:globo g} {}))]
      (is (= [:ok true] result))
      (is (= 1 (count @sent)))
      (let [event (recorded-event sent)]
        (is (= (set (map :model-id placeables/default-config))
               (set (map :model-id (:objects (:content event))))))))))

(ns galt.world-map.domain.use-cases.send-globo-message-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [galt.world-map.domain.use-cases.send-globo-message :as uc]
   [is.galt.globo.protocols :as protocols]
   [is.galt.globo.server :as globo]))

(defn make-globo []
  (let [g (globo/create-globo {:mount-path "/world-map"})]
    (protocols/update-user! (:storage g) "member-1"
                            #(assoc % :id "member-1" :name "Me" :favorites []))
    (protocols/add-user-connection! (:storage g) "member-1" "conn-1")
    (protocols/add-connection! (:connections g) "conn-1" :channel-1)
    g))

(deftest send-globo-message-use-case-test
  (testing "valid message passes through"
    (let [g (make-globo)
          sent (atom [])
          result (with-redefs [org.httpkit.server/send! (fn [ch data & _] (swap! sent conj [ch data]) true)]
                   (uc/send-globo-message-use-case
                    {:globo g}
                    {:type :new-message :user-id "member-1"
                     :content {:text "hello"}}))]
      (is (= [:ok true] result))
      (is (= 1 (count @sent)))))
  (testing "unknown user rejected"
    (let [g (make-globo)
          sent (atom [])
          result (with-redefs [org.httpkit.server/send! (fn [ch data & _] (swap! sent conj [ch data]) true)]
                   (uc/send-globo-message-use-case
                    {:globo g}
                    {:type :new-message :user-id "ghost"
                     :content {:text "hi"}}))]
      (is (= [:error nil [:unknown-user]] result))
      (is (empty? @sent))))
  (testing "invalid message rejected by validation"
    (let [g (make-globo)
          result (uc/send-globo-message-use-case
                  {:globo g}
                  {:type :new-message :user-id "member-1"
                   :content {:text 42}})]
      (is (= :error (first result)))
      (is (some? (nth result 2))))))

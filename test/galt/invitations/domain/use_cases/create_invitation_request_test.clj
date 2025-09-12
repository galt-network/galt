(ns galt.invitations.domain.use-cases.create-invitation-request-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [matcher-combinators.test]
   [clojure.test.check.generators :as gen]
   [clojure.string :as str]
   [spy.core :as spy]
   [spy.assert :as assert]
   [spy.test]
   [galt.invitations.domain.entities.invitation-request :as ir]
   [galt.invitations.domain.use-cases.create-invitation-request :refer [create-invitation-request-use-case]]))

(declare match?)

(defn generate-string-of-length [length]
  (gen/generate (gen/fmap str/join (gen/vector gen/char-alphanumeric length))))

(deftest create-invitation-request-use-case-test
  (testing "error cases"
    (let [deps {:find-user-by-id (constantly {:id (random-uuid)})
                :user-invitation-requests (constantly [{} {} {}])}
          [status _] (create-invitation-request-use-case deps {})]
      (is (= :error status) "Errors for unknown user"))

    (let [user-id (random-uuid)
          user {:id user-id}
          deps {:find-user-by-id (constantly user)
                :user-invitation-requests (constantly [{} {} {} {}])}
          [status _] (create-invitation-request-use-case deps {:from-user-id random-uuid})]
      (is (= :error status) "Errors if too many previous requests"))

    (let [user {:id (random-uuid)}
          deps {:find-user-by-id (constantly user)
                :user-invitation-requests (constantly [{} {} {}])
                :add-invitation-request (constantly nil)}
          command {:from-user user
                   :to-member "member-id"
                   :to-group "group-id"
                   :email "c@example.com"
                   :content (generate-string-of-length 25)}
          [status _] (create-invitation-request-use-case deps command)]
      (is (= :error status) "Errors if request content too short")))

  (testing "success cases"
    (let [user-id (random-uuid)
          target-member-id (random-uuid)
          user {:id user-id}
          invitation-request-storage (constantly
                                       (ir/map->InvitationRequest {:requesting-user-id user-id
                                                                   :target-member-id target-member-id}))
          spy-storage (spy/spy invitation-request-storage)
          deps {:find-user-by-id (constantly user)
                :user-invitation-requests (constantly [{} {} {}])
                :add-invitation-request spy-storage}
          command {:from-user-id user-id
                   :to-member-id target-member-id
                   :to-group-id "group-id"
                   :email "c@example.com"
                   :content (generate-string-of-length 31)}
          [status result] (create-invitation-request-use-case deps command)]
      (is (= :ok status) (str "ok status with valid params: " result))
      (spy.assert/called? spy-storage)
      (is (= (:requesting-user-id result) user-id) "invitation from the correct user")
      (is (= (:target-member-id result) target-member-id) "invitation to the designated member"))))

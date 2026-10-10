(ns galt.members.domain.use-cases.complete-lnurl-login-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [galt.members.domain.use-cases.complete-lnurl-login :refer [complete-lnurl-login-use-case]]
   [java-time.api :as jt]
   [ring.middleware.session.store :as ss]))

(def k1 "29fb94c36f715a92a003eb2c7fbf216c726397b0c7fe2dbe187cebbd2d7128e9")

(def user-id #uuid "00000000-0000-0000-0000-000000000001")

(defn- mem-session-store [sessions]
  (reify ss/SessionStore
    (read-session [_ key] (get @sessions key))
    (write-session [_ key data] (swap! sessions assoc key data))
    (delete-session [_ key] (swap! sessions dissoc key))))

(defn- run-login
  "Runs the use case with a valid challenge and signature. Overrides let tests
   control the user, member and current membership payment."
  [{:keys [user member payment challenge expires-at verify-signature]
    :or {challenge k1
         expires-at (+ (System/currentTimeMillis) 60000)
         verify-signature (fn [_ _ _] true)}}]
  (let [sessions (atom {:sid {:lnurl-auth {:k1 k1 :expires-at expires-at}}})
        deps {:session-store (mem-session-store sessions)
              :verify-signature verify-signature
              :gen-uuid (constantly user-id)
              :find-user-by-pub-key (constantly user)
              :find-member-by-user-id (constantly member)
              :current-membership-payment (constantly payment)
              :add-user (fn [id pub-key] {:id id :pub-key pub-key})}
        result (complete-lnurl-login-use-case
                deps
                {:challenge challenge
                 :signed-challenge "signed"
                 :user-pub-key "02somekey"
                 :session-id :sid})]
    {:result result :sessions @sessions}))

(deftest complete-lnurl-login-new-user-without-payment-test
  (testing "a brand new user (no member profile, no payment) logs in as inactive"
    (let [{:keys [result sessions]} (run-login {})
          [status value] result]
      (is (= :ok status))
      (is (false? (get-in value [:session :membership-active?])))
      (is (nil? (get-in value [:session :member-id])))
      (is (= user-id (get-in value [:session :user-id])))
      (is (false? (get-in sessions [:sid :membership-active?]))))))

(deftest complete-lnurl-login-member-without-payment-test
  (testing "a user with a member profile but no paid membership is inactive"
    (let [{:keys [result]} (run-login {:user {:id user-id} :member {:id user-id}})
          [status value] result]
      (is (= :ok status))
      (is (false? (get-in value [:session :membership-active?])))
      (is (= user-id (get-in value [:session :member-id]))))))

(deftest complete-lnurl-login-active-membership-test
  (testing "a paid membership less than a year old is active"
    (let [paid-at (jt/minus (jt/local-date-time) (jt/months 1))
          {:keys [result]} (run-login {:user {:id user-id}
                                       :member {:id user-id}
                                       :payment {:paid-at paid-at}})
          [status value] result]
      (is (= :ok status))
      (is (true? (get-in value [:session :membership-active?]))))))

(deftest complete-lnurl-login-expired-membership-test
  (testing "a paid membership more than a year old is inactive"
    (let [paid-at (jt/minus (jt/local-date-time) (jt/years 2))
          {:keys [result]} (run-login {:user {:id user-id}
                                       :member {:id user-id}
                                       :payment {:paid-at paid-at}})
          [status value] result]
      (is (= :ok status))
      (is (false? (get-in value [:session :membership-active?]))))))

(deftest complete-lnurl-login-expired-challenge-test
  (testing "an expired challenge is rejected"
    (let [{:keys [result]} (run-login {:expires-at (- (System/currentTimeMillis) 1000)})
          [status _] result]
      (is (= :error status)))))

(deftest complete-lnurl-login-invalid-signature-test
  (testing "an invalid signature is rejected"
    (let [{:keys [result]} (run-login {:verify-signature (fn [_ _ _] false)})
          [status _] result]
      (is (= :error status)))))

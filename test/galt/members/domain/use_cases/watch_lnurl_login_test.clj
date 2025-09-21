(ns galt.members.domain.use-cases.watch-lnurl-login-test
  (:require
   [clojure.test :refer [deftest is]]
   [galt.members.domain.use-cases.watch-lnurl-login :refer [watch-lnurl-login-use-case]]))

(deftest galt.members.domain.use-cases.watch-lnurl-login-test
  (let [sessions {:logged-out nil
                  :expired {:lnurl-auth {:expires-at (- (System/currentTimeMillis) 10000)}}
                  :in-progress {:lnurl-auth {:expires-at (+ (System/currentTimeMillis) 10000)}}
                  :logged-in {:user-id 42}}
        use-case (partial watch-lnurl-login-use-case {:read-session sessions})
        status (fn [x] (:status (second x)))]
    (is (= :logged-out (status (use-case {:session-id :logged-out}))))
    (is (= :expired (status (use-case {:session-id :expired}))))
    (is (= :in-progress (status (use-case {:session-id :in-progress}))))
    (is (= :logged-in (status (use-case {:session-id :logged-in}))))
    (is (not (= :logged-in (status (use-case {:session-id :logged-out})))))))

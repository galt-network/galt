(ns galt.locations.adapters.client-ip-test
  (:require
   [clojure.test :refer [deftest is]]
   [galt.locations.adapters.client-ip :as client-ip]))

(deftest prefers-cf-connecting-ip
  (is (= "1.2.3.4"
         (client-ip/client-ip {:headers {"cf-connecting-ip" "1.2.3.4"
                                         "x-real-ip" "9.9.9.9"}
                               :remote-addr "10.0.0.1"}))))

(deftest falls-back-to-x-forwarded-for-then-remote-addr
  (is (= "5.6.7.8"
         (client-ip/client-ip {:headers {"x-forwarded-for" "5.6.7.8, 10.0.0.1"}
                               :remote-addr "10.0.0.1"})))
  (is (= "10.0.0.1"
         (client-ip/client-ip {:headers {} :remote-addr "10.0.0.1"}))))

(deftest loopback-uses-cached-public-ip
  (client-ip/clear-public-ip-cache!)
  (let [calls (atom 0)]
    (with-redefs [client-ip/fetch-public-ip (fn [] (swap! calls inc) "8.8.8.8")]
      (is (= "8.8.8.8" (client-ip/client-ip {:headers {} :remote-addr "127.0.0.1"})))
      (is (= "8.8.8.8" (client-ip/client-ip {:headers {} :remote-addr "::1"})))
      (is (= 1 @calls)))))

(deftest non-loopback-skips-public-ip-lookup
  (client-ip/clear-public-ip-cache!)
  (let [calls (atom 0)]
    (with-redefs [client-ip/fetch-public-ip (fn [] (swap! calls inc) "8.8.8.8")]
      (is (= "10.0.0.1" (client-ip/client-ip {:headers {} :remote-addr "10.0.0.1"})))
      (is (zero? @calls)))))

(deftest public-ip-fetch-failure-returns-nil
  (client-ip/clear-public-ip-cache!)
  (with-redefs [client-ip/fetch-public-ip (constantly nil)]
    (is (nil? (client-ip/client-ip {:headers {} :remote-addr "127.0.0.1"})))))

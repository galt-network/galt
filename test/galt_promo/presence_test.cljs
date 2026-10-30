(ns galt-promo.presence-test
  (:require [cljs.test :refer [deftest is testing]]
            [galt-promo.presence :as presence]))

(deftest disconnected-user-expiry
  (let [users {"connected" {:connected? true}
               "recent" {:connected? false :disconnected-at 1000}
               "expired" {:connected? false :disconnected-at 0}}]
    (testing "connected users and users inside the grace period remain"
      (is (= #{"connected" "recent"}
             (set (keys (presence/prune-disconnected users 10999))))))
    (testing "users are removed at ten seconds"
      (is (= #{"connected"}
             (set (keys (presence/prune-disconnected users 11000))))))))

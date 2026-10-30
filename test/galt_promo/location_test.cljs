(ns galt-promo.location-test
  (:require [cljs.test :refer [deftest is testing]]
            [galt-promo.location :as location]))

(deftest browser-coordinates-are-validated
  (testing "numeric strings become coordinates"
    (is (= {:lat 40.7128 :lng -74.006}
           (location/parse-coordinates "40.7128" "-74.006"))))
  (testing "valid boundaries are accepted"
    (is (= {:lat -90 :lng 180}
           (location/parse-coordinates "-90" "180"))))
  (testing "invalid coordinates are rejected"
    (is (nil? (location/parse-coordinates "91" "0")))
    (is (nil? (location/parse-coordinates "0" "-181")))
    (is (nil? (location/parse-coordinates "abc" "0")))
    (is (nil? (location/parse-coordinates nil "0")))))

(deftest public-ips-resolve-locally
  (let [{:keys [lat lng source]} (location/ip-location "8.8.8.8")]
    (is (number? lat))
    (is (number? lng))
    (is (= :ip source)))
  (is (nil? (location/ip-location "127.0.0.1"))))

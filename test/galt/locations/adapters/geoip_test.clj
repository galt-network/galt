(ns galt.locations.adapters.geoip-test
  (:require
   [clojure.test :refer [deftest is]]
   [galt.locations.adapters.geoip :as geoip]))

(deftest missing-mmdb-returns-nil
  (is (nil? (geoip/lookup "/no/such/geoip.mmdb" "1.2.3.4"))))

(deftest prefers-member-location-over-ip
  (let [infer (geoip/make-infer-location
               {:mmdb-path "/no/such/geoip.mmdb"
                :find-member (fn [id] (when (= id 7) {:id 7 :location-id 3}))
                :find-location (fn [id] (when (= id 3) {:id 3 :name "Tartu" :latitude 58.38 :longitude 26.72}))})
        loc (infer {:session {:member-id 7} :headers {} :remote-addr "1.2.3.4"})]
    (is (= "Tartu" (:label loc)))
    (is (= 58.38 (:lat loc)))
    (is (= 100 (:radius-km loc)))))

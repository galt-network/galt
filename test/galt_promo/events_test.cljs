(ns galt-promo.events-test
  (:require [cljs.test :refer [deftest is]]
            [galt-promo.events :as events]
            [re-frame.core :as rf]))

(deftest user-snapshots-update-subscriptions
  (rf/dispatch-sync [::events/initialize])
  (rf/dispatch-sync [::events/users-received [{:id "one" :name "Amber Alpaca"}]])
  (rf/dispatch-sync [::events/connection-changed :open])
  (is (= [{:id "one" :name "Amber Alpaca"}] @(rf/subscribe [::events/users])))
  (is (= :open @(rf/subscribe [::events/connection]))))

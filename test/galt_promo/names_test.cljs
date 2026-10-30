(ns galt-promo.names-test
  (:require [cljs.test :refer [deftest is testing]]
            [galt-promo.names :as names]))

(deftest generated-names
  (testing "two words share an initial"
    (is (= "Amber Alpaca" (names/generate-name #{} (constantly 0)))))
  (testing "an existing name is skipped"
    (is (= "Amber Antelope"
           (names/generate-name #{"Amber Alpaca"} (constantly 0))))))

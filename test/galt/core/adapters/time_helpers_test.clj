(ns galt.core.adapters.time-helpers-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [java-time.api :as jt]
   [galt.core.adapters.time-helpers :refer [period-range]]))

(deftest time-helpers.period-range-tests
  (testing ":this-week"
    (let [monday (jt/local-date-time 2025 9 29 0 1)
          sunday (jt/plus monday (jt/days 6))
          next-monday (jt/plus monday (jt/days 7))
          correct-start (jt/local-date-time 2025 9 29 0 0)
          correct-end (jt/local-date-time 2025 10 6 0 0)]
      (is (= [correct-start correct-end] (period-range monday :this-week)))
      (is (= [correct-start correct-end] (period-range sunday :this-week)))
      (is (not (= [correct-start correct-end] (period-range next-monday :this-week))))))

  (testing ":this-year"
    (let [monday (jt/local-date-time 2025 9 29 0 1)
          sunday (jt/plus monday (jt/days 6))
          next-year-monday (jt/plus monday (jt/days 365))
          correct-start (jt/local-date-time 2025 1 1 0 0)
          correct-end (jt/local-date-time 2026 1 1 0 0)]
      (is (= [correct-start correct-end] (period-range monday :this-year)))
      (is (= [correct-start correct-end] (period-range sunday :this-year)))
      (is (not (= [correct-start correct-end] (period-range next-year-monday :this-week)))))))

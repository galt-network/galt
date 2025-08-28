(ns galt.core.adapters.time-helpers
  (:require
    [java-time.api :as jt]
    [clojure.string :as str])
  (:import
    [java.time ZonedDateTime]))

(defn relative-time
  ([^ZonedDateTime zdt] (relative-time zdt (jt/zoned-date-time)))
  ([^ZonedDateTime zdt ^ZonedDateTime now]
    (let [direction (fn [diff unit-str]
                      (let [abs-diff (Math/abs diff)
                            plural (when (not= 1 abs-diff) "s")]
                        (cond
                          (> diff 0) (str abs-diff " " unit-str plural " ago")
                          (< diff 0) (str "in " abs-diff " " unit-str plural)
                          :else "now")))
          years (jt/time-between zdt now :years)
          months (jt/time-between zdt now :months)
          weeks (jt/time-between zdt now :weeks)
          days (jt/time-between zdt now :days)
          hours (jt/time-between zdt now :hours)
          minutes (jt/time-between zdt now :minutes)
          seconds (jt/time-between zdt now :seconds)]
        (cond
          (not= 0 years) (direction years "year")
          (not= 0 months) (direction months "month")
          (not= 0 weeks) (direction weeks "week")
          (not= 0 days) (direction days "day")
          (not= 0 hours) (direction hours "hour")
          (not= 0 minutes) (direction minutes "minute")
          (not= 0 seconds) (direction seconds "second")
          :else "just now"))))

(defn short-format
  [datetime]
  (jt/format :iso-local-date datetime))

(defn num->ordinal-str [n]
  (let [last-digit (mod n 10)
        last-two-digits (mod n 100)]
    (str n
         (cond
           (or (= last-two-digits 11)
               (= last-two-digits 12)
               (= last-two-digits 13)) "th"
           (= last-digit 1) "st"
           (= last-digit 2) "nd"
           (= last-digit 3) "rd"
           :else "th"))))

(defn long-format
  [datetime]
  (let [day (jt/format "EEEE" datetime)
        {:keys [day-of-month]} (jt/as-map datetime)
        month-day (num->ordinal-str day-of-month)
        month (jt/format "LLLL" datetime)
        year (jt/format "uuuu" datetime)]
    (str day ", " (str/join " " [month-day "of" month year]))))

(comment
  (require '[clojure.java-time.api :as jt])
  (def created-at (jt/zoned-date-time))
  (num->ordinal-str 42)
  (long-format created-at)
  (short-format created-at)
  (jt/as-map (jt/zoned-date-time))
  (relative-time created-at created-at)
  (relative-time (jt/zoned-date-time 2025 1 1) (jt/zoned-date-time 2024 6 1)))

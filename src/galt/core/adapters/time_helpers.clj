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

;; Formatting strings from https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html
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

(defn long-format-with-time
  [datetime]
  (let [day (jt/format "EEE" datetime)
        {:keys [day-of-month]} (jt/as-map datetime)
        month-day (num->ordinal-str day-of-month)
        month (jt/format "LLL" datetime)
        year (jt/format "uu" datetime)
        time (jt/format "HH:mm" datetime)]
    (str time " on " day ", " (str/join " " [month-day "of" month "'" year]))))

(defn relative-with-short
  "Returns a date string like 3 days ago (2025-09-24)"
  [datetime]
  (str (relative-time datetime) " (" (short-format datetime) ")"))

(defn timestamp-id
  "Returns a string like 20250920133056 with 1/100 second precision
  Useful for generating somewhat unique ID-s"
  []
  (jt/format "yyyyMMddHHmmssSS" (jt/local-date-time)))

(defn parse-form-datetime
  [s]
  (jt/local-date-time s))

(defn period-range
  "Returns a map with :start-time and :end-time as LocalDateTime for the given period keyword."
  [kw]
  {:pre [(keyword? kw)]}
  (let [now (jt/local-time)
        today (jt/local-date now)
        midnight (jt/local-time 0 0)
        dow-value (.getValue (jt/day-of-week today))
        days-back (jt/days (dec dow-value))
        monday (jt/minus today days-back)
        saturday (jt/plus monday (jt/days 5))
        next-monday (jt/plus monday (jt/days 7))
        first-month (jt/adjust today :first-day-of-month)
        next-first-month (jt/adjust today :first-day-of-next-month)
        tomorrow (jt/plus today (jt/days 1))
        day-after (jt/plus today (jt/days 2))]
    (case kw
      :today {:start-time (jt/local-date-time today midnight)
              :end-time (jt/local-date-time tomorrow midnight)}
      :tomorrow {:start-time (jt/local-date-time tomorrow midnight)
                 :end-time (jt/local-date-time day-after midnight)}
      :this-week {:start-time (jt/local-date-time monday midnight)
                  :end-time (jt/local-date-time next-monday midnight)}
      :this-weekend {:start-time (jt/local-date-time saturday midnight)
                     :end-time (jt/local-date-time next-monday midnight)}
      :next-week {:start-time (jt/local-date-time next-monday midnight)
                  :end-time (jt/local-date-time (jt/plus next-monday (jt/days 7)) midnight)}
      :this-month {:start-time (jt/local-date-time first-month midnight)
                   :end-time (jt/local-date-time next-first-month midnight)}
      :all {}
      (throw (ex-info "Invalid period keyword" {:kw kw})))))
(comment
  (require '[clojure.java-time.api :as jt])

  (def created-at (jt/zoned-date-time))
  (num->ordinal-str 42)
  (long-format created-at)
  (short-format created-at)
  (jt/java-date (jt/instant 1759002549000))
  (jt/instant 1759002549000)
  (jt/java-date 1759002549000)
  (jt/local-date-time)
  (jt/as-map (jt/zoned-date-time))
  (relative-time created-at created-at)
  (jt/format "yyyyMMddHHmmssSS" created-at)
  (jt/format "yyyyMMddHHmmssSS" (jt/local-date-time))
  (relative-time (jt/zoned-date-time 2025 1 1) (jt/zoned-date-time 2024 6 1))
  (period-range :today)
  (period-range :this-month)
  (jt/adjust (jt/local-date-time) :day-of-week)
  )

(ns galt.connect.domain.use-cases.search-test
  (:require
    [clojure.test :refer [deftest is]]
    [galt.connect.adapters.memory-search-repository :as memory]
    [galt.connect.domain.use-cases.search :as search]
    [java-time.api :as jt]))

(def mixed-hits
  [{:type :member :id 1 :title "Ada" :description "Engineer" :activity-at 4}
   {:type :group :id 2 :title "Builders" :description "Make things" :activity-at 3}
   {:type :event :id 3 :title "Meetup" :description "Friday" :activity-at 2}
   {:type :post :id 4 :title "Hello" :description "First post" :activity-at 1}])

(defn- search-with [hits command]
  (search/search {:search-repo (memory/new-memory-search-repository hits)} command))

(deftest empty-command-returns-mixed-hits
  (let [[status result] (search-with mixed-hits {})]
    (is (= :ok status))
    (is (= #{:member :group :event :post} (set (map :type result))))))

(deftest query-matches-title-or-description
  (let [[_ result] (search-with mixed-hits {:query "build"})]
    (is (= ["Builders"] (map :title result))))
  (let [[_ result] (search-with mixed-hits {:query "engineer"})]
    (is (= ["Ada"] (map :title result)))))

(deftest types-drop-other-kinds
  (let [[_ result] (search-with mixed-hits {:types #{:event :post}})]
    (is (= #{:event :post} (set (map :type result))))))

(deftest ranks-by-activity-at-descending
  (let [unordered (reverse mixed-hits)
        [_ result] (search-with unordered {})]
    (is (= [4 3 2 1] (map :activity-at result)))))

(deftest pages-after-rank
  (let [unordered (reverse mixed-hits)
        [_ result] (search-with unordered {:offset 1 :limit 2})]
    (is (= [3 2] (map :activity-at result)))))

(deftest period-keeps-in-range-events-and-posts
  (let [from (jt/local-date-time 2026 8 17 0 0)
        to (jt/local-date-time 2026 8 24 0 0)
        hits [{:type :member :id 1 :title "Ada" :activity-at 5}
              {:type :group :id 2 :title "Builders" :activity-at 4}
              {:type :event :id 3 :title "This week" :activity-at 3
               :start-time (jt/local-date-time 2026 8 20 18 0)}
              {:type :event :id 4 :title "Next year" :activity-at 2
               :start-time (jt/local-date-time 2027 1 1 18 0)}
              {:type :post :id 5 :title "Fresh" :activity-at 1
               :publish-at (jt/local-date-time 2026 8 18 12 0)}
              {:type :post :id 6 :title "Old" :activity-at 0
               :publish-at (jt/local-date-time 2025 1 1 12 0)}]
        [_ result] (search-with hits {:from from :to to})]
    (is (= #{"Ada" "Builders" "This week" "Fresh"} (set (map :title result))))))

(deftest location-keeps-hits-within-radius
  (let [hits [{:type :member :id 1 :title "Near" :activity-at 2 :lat 0.5 :lng 0.0}
              {:type :group :id 2 :title "Far" :activity-at 1 :lat 2.0 :lng 0.0}
              {:type :event :id 3 :title "Nowhere" :activity-at 0}]
        [_ result] (search-with hits {:location {:lat 0.0 :lng 0.0 :radius-km 100}})]
    (is (= ["Near"] (map :title result)))))

(deftest hidden-hits-are-excluded
  (let [hits [{:type :member :id 1 :title "Visible" :activity-at 2}
              {:type :member :id 2 :title "Ghost" :activity-at 1 :hidden? true}]
        [_ result] (search-with hits {})]
    (is (= ["Visible"] (map :title result)))))

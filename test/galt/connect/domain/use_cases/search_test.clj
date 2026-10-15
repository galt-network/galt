(ns galt.connect.domain.use-cases.search-test
  (:require
    [clojure.string :as str]
    [clojure.test :refer [deftest is]]
    [galt.connect.adapters.memory-search-repository :as memory]
    [galt.connect.domain.search-repository :as search-repo]
    [galt.connect.domain.use-cases.search :as search]
    [java-time.api :as jt]))

(def mixed-hits
  [{:type :member :id 1 :title "Ada" :description "Engineer" :activity-at 4}
   {:type :group :id 2 :title "Builders" :description "Make things" :activity-at 3}
   {:type :event :id 3 :title "Meetup" :description "Friday" :activity-at 2}
   {:type :post :id 4 :title "Hello" :description "First post" :activity-at 1}])

(defn- search-with [hits command]
  (search/search {:search-repo (memory/new-memory-search-repository hits)} command))

(defn- search-hits [hits command]
  (:hits (second (search-with hits command))))

(deftest search-reports-hits-and-whether-near-me-was-dropped
  (let [[status result] (search-with mixed-hits {})]
    (is (= :ok status))
    (is (false? (:near-me-dropped? result)))
    (is (= [4 3 2 1] (map :activity-at (:hits result))))))

(deftest empty-command-returns-mixed-hits
  (is (= #{:member :group :event :post} (set (map :type (search-hits mixed-hits {}))))))

(def text-hits
  [{:type :member :id 3 :title "Ada" :description "Builds robots" :activity-at 3}
   {:type :event :id 1 :title "Bitcoin Meetup" :description "Monthly gathering" :activity-at 2}
   {:type :post :id 2 :title "Weekly notes" :description "We talked about bitcoin" :activity-at 1}])

(def typo-hits
  [{:type :member :id 1 :title "Rustaceans" :description "We meet to talk about code" :activity-at 1}
   {:type :event :id 2 :title "Bitcoin Meetup" :description "One hour of talks" :activity-at 2}
   {:type :group :id 3 :title "Weekly Notes" :description "Writers corner" :activity-at 3}
   {:type :post :id 4 :title "Monthly Gathering" :description "Agenda inside" :activity-at 4}])

(def origin {:lat 0.0 :lng 0.0 :radius-km 100})

(def near-and-far
  [{:type :member :id 1 :title "Local Pottery" :description "Tartu studio" :activity-at 2
    :lat 0.05 :lng 0.0}
   {:type :event :id 2 :title "Distant Bitcoin" :description "Tallinn meetup" :activity-at 1
    :lat 40.0 :lng 40.0}])

(defn- dropped-near-me? [hits command]
  (:near-me-dropped? (second (search-with hits command))))

(deftest query-matches-whole-words-in-any-order
  (is (= ["Bitcoin Meetup"] (map :title (search-hits text-hits {:query "meetup bitcoin"}))))
  (is (= ["Bitcoin Meetup"] (map :title (search-hits text-hits {:query "monthly meetup"})))))

(deftest query-matching-ignores-case
  (is (= ["Bitcoin Meetup"] (map :title (search-hits text-hits {:query "BITCOIN MEETUP"})))))

(deftest a-word-does-not-match-inside-another-word
  (is (empty? (search-hits text-hits {:query "bit"}))))

(deftest every-word-is-required
  (is (empty? (search-hits text-hits {:query "bitcoin robots"}))))

(deftest an-english-stem-matches-the-typed-word
  (is (= ["Bitcoin Meetup"] (map :title (search-hits text-hits {:query "meetups"})))))

(deftest a-title-match-ranks-above-a-newer-body-mention
  (let [hits [{:type :member :id 1 :title "Ada" :description "Talks about bitcoin" :activity-at 9}
              {:type :event :id 2 :title "Bitcoin Meetup" :description "Talk" :activity-at 1}]]
    (is (= ["Bitcoin Meetup" "Ada"] (map :title (search-hits hits {:query "bitcoin"}))))))

(deftest activity-at-breaks-ties-when-a-query-is-present
  (let [hits [{:type :event :id 1 :title "Bitcoin A" :activity-at 1}
              {:type :event :id 2 :title "Bitcoin B" :activity-at 5}]]
    (is (= ["Bitcoin B" "Bitcoin A"] (map :title (search-hits hits {:query "bitcoin"}))))))

(deftest pages-continue-the-match-order
  (let [hits [{:type :event :id 1 :title "Bitcoin" :activity-at 1}
              {:type :post :id 2 :title "Warm" :description "Bitcoin news" :activity-at 5}
              {:type :post :id 3 :title "Cold" :description "Bitcoin talk" :activity-at 4}]
        page (fn [offset] (map :title (search-hits hits {:query "bitcoin" :limit 2 :offset offset})))]
    (is (= ["Bitcoin" "Warm"] (page 0)))
    (is (= ["Cold"] (page 2)))))

(deftest a-blank-or-punctuation-only-query-is-the-activity-feed
  (is (= [3 2 1] (map :activity-at (search-hits text-hits {:query "   "}))))
  (is (= [3 2 1] (map :activity-at (search-hits text-hits {:query "?!."})))))

(deftest a-matching-hit-shows-a-snippet-with-the-matched-words
  (let [hit (first (search-hits text-hits {:query "gathering"}))]
    (is (= "Bitcoin Meetup" (:title hit)))
    (is (str/includes? (:snippet hit) "gathering"))))

(deftest a-snippet-survives-punctuation-around-the-matched-word
  (let [hits [{:type :post :id 1 :title "Notes" :description "We talked about bitcoin." :activity-at 1}]
        hit (first (search-hits hits {:query "bitcoin"}))]
    (is (= :body (:match hit)))
    (is (str/includes? (:snippet hit) "bitcoin"))))

(deftest a-snippet-is-not-added-without-a-query
  (is (every? #(nil? (:snippet %)) (search-hits text-hits {}))))

(deftest types-drop-other-kinds
  (is (= #{:event :post} (set (map :type (search-hits mixed-hits {:types #{:event :post}}))))))

(deftest ranks-by-activity-at-descending
  (is (= [4 3 2 1] (map :activity-at (search-hits (reverse mixed-hits) {})))))

(deftest pages-after-rank
  (is (= [3 2] (map :activity-at (search-hits (reverse mixed-hits) {:offset 1 :limit 2})))))

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
               :publish-at (jt/local-date-time 2025 1 1 12 0)}]]
    (is (= #{"Ada" "Builders" "This week" "Fresh"}
           (set (map :title (search-hits hits {:from from :to to})))))))

(deftest location-keeps-hits-within-radius
  (let [hits [{:type :member :id 1 :title "Near" :activity-at 2 :lat 0.5 :lng 0.0}
              {:type :group :id 2 :title "Far" :activity-at 1 :lat 2.0 :lng 0.0}
              {:type :event :id 3 :title "Nowhere" :activity-at 0}]]
    (is (= ["Near"] (map :title (search-hits hits {:location {:lat 0.0 :lng 0.0 :radius-km 100}}))))))

(deftest hidden-hits-are-excluded
  (let [hits [{:type :member :id 1 :title "Visible" :activity-at 2}
              {:type :member :id 2 :title "Ghost" :activity-at 1 :hidden? true}]]
    (is (= ["Visible"] (map :title (search-hits hits {}))))))

(deftest deleted-members-are-excluded
  (let [hits [{:type :member :id 1 :title "Visible" :activity-at 2}
              {:type :member :id 2 :title "Removed" :activity-at 1 :deleted? true}
              {:type :group :id 3 :title "Keeper" :activity-at 0 :deleted? true}]]
    (is (= ["Visible" "Keeper"] (map :title (search-hits hits {}))))))

(deftest a-mistyped-member-name-still-returns-that-member
  (is (= ["Rustaceans"] (map :title (search-hits typo-hits {:query "rustaceons"})))))

(deftest a-mistyped-group-name-still-returns-that-hit
  (is (= ["Weekly Notes"] (map :title (search-hits typo-hits {:query "weekley"})))))

(deftest a-mistyped-event-name-still-returns-that-hit
  (is (= ["Bitcoin Meetup"] (map :title (search-hits typo-hits {:query "bitcoinn"})))))

(deftest a-mistyped-post-title-still-returns-that-hit
  (is (= ["Monthly Gathering"] (map :title (search-hits typo-hits {:query "gatherring"})))))

(deftest a-different-word-is-not-a-typo
  (is (empty? (search-hits typo-hits {:query "meeting"}))))

(deftest a-typo-in-a-description-does-not-satisfy-the-query
  (is (empty? (search-hits [{:type :event :id 1 :title "Bitcoin Meetup"
                             :description "Monthly gathering" :activity-at 1}]
                           {:query "gatherring"}))))

(deftest a-typo-can-satisfy-one-word-of-a-multi-word-query
  (is (= ["Bitcoin Meetup"] (map :title (search-hits typo-hits {:query "bitcoinn meetup"})))))

(deftest a-typo-that-only-lands-in-a-description-does-not-satisfy-the-query
  (is (empty? (search-hits [{:type :post :id 1 :title "Weekly Notes"
                             :description "Monthly gathering" :activity-at 1}]
                           {:query "weekly gatherring"}))))

(deftest a-typo-match-still-shows-a-snippet
  (let [hit (first (search-hits typo-hits {:query "bitcoinn"}))]
    (is (str/includes? (:snippet hit) "Bitcoin"))))

(deftest an-exact-name-with-punctuation-is-found
  (let [hits [{:type :member :id 1 :title "Jean-Luc" :description "Cyclist" :activity-at 1}
              {:type :member :id 2 :title "Ada" :description "Engineer" :activity-at 2}]]
    (is (= ["Jean-Luc"] (map :title (search-hits hits {:query "Jean-Luc"}))))))

(deftest an-exact-title-match-ranks-above-a-fuzzy-one
  (let [hits [{:type :group :id 1 :title "Rustaceans" :description "Code group" :activity-at 1}
              {:type :event :id 2 :title "Rustaceons" :description "Once a year" :activity-at 9}]]
    (is (= ["Rustaceans" "Rustaceons"] (map :title (search-hits hits {:query "rustaceans"}))))))

(deftest a-query-with-no-local-hits-is-retried-without-near-me
  (is (= ["Distant Bitcoin"] (map :title (search-hits near-and-far {:query "bitcoin" :location origin}))))
  (is (dropped-near-me? near-and-far {:query "bitcoin" :location origin})))

(deftest the-retry-is-reported-even-when-it-finds-nothing
  (is (empty? (search-hits near-and-far {:query "bitcoin pottery" :location origin})))
  (is (dropped-near-me? near-and-far {:query "bitcoin pottery" :location origin})))

(deftest an-empty-query-never-drops-near-me
  (is (= ["Local Pottery"] (map :title (search-hits near-and-far {:query "  " :location origin}))))
  (is (not (dropped-near-me? near-and-far {:query "  " :location origin})))
  (is (not (dropped-near-me? near-and-far {:query "?!." :location origin}))))

(deftest a-query-with-local-hits-stays-inside-the-radius
  (let [hits [{:type :member :id 1 :title "Local Bitcoin" :activity-at 2 :lat 0.05 :lng 0.0}
              {:type :event :id 2 :title "Distant Bitcoin" :activity-at 9 :lat 40.0 :lng 40.0}]]
    (is (= ["Local Bitcoin"] (map :title (search-hits hits {:query "bitcoin" :location origin}))))
    (is (not (dropped-near-me? hits {:query "bitcoin" :location origin})))))

(deftest an-explicit-location-follows-the-one-retry-rule
  (let [place (assoc origin :label "Tartu")]
    (is (= ["Distant Bitcoin"] (map :title (search-hits near-and-far {:query "bitcoin" :location place}))))
    (is (dropped-near-me? near-and-far {:query "bitcoin" :location place}))))

(deftest later-pages-of-a-dropped-search-stay-outside-the-radius
  (let [hits (mapv (fn [i] {:type :event :id i :title (str "Bitcoin " i)
                            :activity-at i :lat 40.0 :lng 40.0})
                   (range 1 13))
        command {:query "bitcoin" :location origin :offset 10 :limit 10}]
    (is (= ["Bitcoin 2" "Bitcoin 1"] (map :title (search-hits hits command))))
    (is (dropped-near-me? hits command))))

(deftest the-search-does-not-retry-a-second-time
  (let [calls (atom 0)
        inner (memory/new-memory-search-repository near-and-far)
        counting (reify search-repo/SearchRepository
                   (candidates [_ command]
                     (swap! calls inc)
                     (search-repo/candidates inner command)))]
    (search/search {:search-repo counting} {:query "bitcoin" :location origin})
    (is (= 2 @calls))))

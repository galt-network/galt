(ns galt.connect.adapters.handlers-test
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]
   [galt.connect.adapters.handlers :as handlers]
    [galt.connect.adapters.memory-search-repository :as memory]
    [galt.core.adapters.sse-helpers :as sse]
    [galt.core.adapters.time-helpers :as th]
    [galt.core.infrastructure.web.helpers :refer [->json]]
    [java-time.api :as jt]
    [ring.middleware.params :refer [wrap-params]]
    [ring.mock.request :as mock]))

(defn- stub-deps [& {:as extra}]
  (merge {:render identity
          :layout (fn [model] model)
          :link-for-route (fn [route & _] (str "/" (subs (str route) 1)))
          :search-repo (memory/new-memory-search-repository
                        [{:type :member :id 1 :title "Ada" :description "Engineer" :activity-at 1}])
          :infer-location (constantly nil)}
         extra))

(defn- call [deps req]
  ((wrap-params (fn [r] (handlers/show-connect deps r))) req))

(defn- capture-sse [_req send-cb]
  (let [sent (atom [])]
    (send-cb (fn [& args] (swap! sent conj (vec args))))
    {:status 200 :body @sent}))

(defn- datastar-req
  [signals]
  (-> (mock/request :get "/connect")
      (mock/header "datastar-request" "true")
      (mock/query-string {"datastar" (->json signals)
                          "patch-mode" "inner"})))

(defn- sent-html [resp]
  (->> (:body resp)
       (filter #(= :html (first %)))
       (map (fn [[_ html opts]] {:html (pr-str html) :opts opts}))))

(deftest connect-page-returns-ok
  (is (= 200 (:status (call (stub-deps) (mock/request :get "/connect"))))))

(deftest connect-page-has-search-box
  (is (str/includes?
       (str (:body (call (stub-deps) (mock/request :get "/connect"))))
       ":name \"query\"")))

(deftest connect-page-lists-hits
  (let [body (str (:body (call (stub-deps) (mock/request :get "/connect"))))]
    (is (str/includes? body "Ada"))
    (is (str/includes? body "/members"))))

(deftest connect-page-filters-by-query
  (let [deps (stub-deps :search-repo
                        (memory/new-memory-search-repository
                         [{:type :member :id 1 :title "Ada" :description "x" :activity-at 2}
                          {:type :group :id 2 :title "Bob's Club" :description "y" :activity-at 1}]))
        body (str (:body (call deps (mock/query-string (mock/request :get "/connect") {:query "Ada"}))))]
    (is (str/includes? body "Ada"))
    (is (not (str/includes? body "Bob's Club")))))

(deftest connect-page-filters-by-types
  (let [deps (stub-deps :search-repo
                        (memory/new-memory-search-repository
                         [{:type :member :id 1 :title "Ada" :description "x" :activity-at 2}
                          {:type :event :id 3 :title "Meetup" :description "y" :activity-at 1}]))
        body (str (:body (call deps (mock/query-string (mock/request :get "/connect") {:types "event"}))))]
    (is (str/includes? body "Meetup"))
    (is (not (str/includes? body "Ada")))))

(deftest connect-page-filters-by-period
  (let [[from to] (th/period-range :this-week)
        later (jt/plus to (jt/days 10))
        deps (stub-deps :search-repo
                        (memory/new-memory-search-repository
                         [{:type :event :id 1 :title "This week" :description "x"
                           :start-time from :activity-at 2}
                          {:type :event :id 2 :title "Later" :description "y"
                           :start-time later :activity-at 1}]))
        body (str (:body (call deps (mock/query-string (mock/request :get "/connect") {:period "this-week"}))))]
    (is (str/includes? body "This week"))
    (is (not (str/includes? body "Later")))))

(deftest connect-page-shows-inferred-location-chip
  (let [deps (stub-deps
              :infer-location (constantly {:lat 59.44 :lng 24.75 :label "Tallinn" :radius-km 100})
              :search-repo (memory/new-memory-search-repository
                            [{:type :member :id 1 :title "Nearby Ada" :description "x"
                              :lat 59.44 :lng 24.75 :activity-at 2}
                             {:type :member :id 2 :title "Far Bob" :description "y"
                              :lat 40.7 :lng -74.0 :activity-at 1}]))
        body (str (:body (call deps (mock/request :get "/connect"))))]
    (is (str/includes? body "Tallinn"))
    (is (str/includes? body "Nearby Ada"))
    (is (not (str/includes? body "Far Bob")))))

(deftest connect-page-dismisses-inferred-location
  (let [deps (stub-deps
              :infer-location (constantly {:lat 59.44 :lng 24.75 :label "Tallinn" :radius-km 100})
              :search-repo (memory/new-memory-search-repository
                            [{:type :member :id 1 :title "Nearby Ada" :description "x"
                              :lat 59.44 :lng 24.75 :activity-at 2}
                             {:type :member :id 2 :title "Far Bob" :description "y"
                              :lat 40.7 :lng -74.0 :activity-at 1}]))
        body (str (:body (call deps (mock/query-string (mock/request :get "/connect") {:geo "off"}))))]
    (is (not (str/includes? body "Tallinn")))
    (is (str/includes? body "Far Bob"))))

(deftest connect-page-uses-explicit-location
  (let [called? (atom false)
        deps (stub-deps
              :infer-location (fn [_] (reset! called? true) nil)
              :search-repo (memory/new-memory-search-repository
                            [{:type :member :id 1 :title "Tartu Ada" :description "x"
                              :lat 58.38 :lng 26.72 :activity-at 2}
                             {:type :member :id 2 :title "NYC Bob" :description "y"
                              :lat 40.7 :lng -74.0 :activity-at 1}]))
        body (str (:body (call deps (mock/query-string (mock/request :get "/connect")
                                                       {:lat "58.38" :lng "26.72" :label "Tartu"}))))]
    (is (str/includes? body "Tartu"))
    (is (str/includes? body "Tartu Ada"))
    (is (not (str/includes? body "NYC Bob")))
    (is (false? @called?))))

(deftest connect-page-shows-near-me-when-geo-off
  (let [body (str (:body (call (stub-deps) (mock/query-string (mock/request :get "/connect") {:geo "off"}))))]
    (is (str/includes? body "Near me"))))

(deftest connect-page-credits-db-ip
  (is (str/includes?
       (str (:body (call (stub-deps) (mock/request :get "/connect"))))
       "IP Geolocation by DB-IP")))

(deftest connect-page-has-infinite-scroll
  (let [body (str (:body (call (stub-deps) (mock/request :get "/connect"))))]
    (is (str/includes? body "data-on-intersect"))
    (is (str/includes? body "patch-mode=append"))))

(deftest connect-checkboxes-refetch-on-change
  (let [body (str (:body (call (stub-deps) (mock/request :get "/connect"))))]
    (is (str/includes? body ":data-bind \"member\""))
    (is (str/includes? body "data-on:change"))
    (is (str/includes? body "patch-mode=inner"))))

(deftest connect-location-chip-refetch-on-click
  (let [deps (stub-deps :infer-location (constantly {:lat 59.44 :lng 24.75 :label "Tallinn" :radius-km 100}))
        body (str (:body (call deps (mock/request :get "/connect"))))]
    (is (str/includes? body "data-on:click"))
    (is (str/includes? body "$geo = 'off'"))))

(deftest connect-near-me-refetch-on-click
  (let [body (str (:body (call (stub-deps) (mock/request :get "/connect"))))]
    (is (str/includes? body "data-on:click"))
    (is (str/includes? body "$geo = ''"))))

(deftest connect-datastar-filters-by-types-signal
  (let [deps (stub-deps :search-repo
                        (memory/new-memory-search-repository
                         [{:type :member :id 1 :title "Ada" :description "x" :activity-at 2}
                          {:type :group :id 2 :title "Bob's Club" :description "y" :activity-at 1}
                          {:type :post :id 4 :title "A Post" :description "z" :activity-at 3}]))]
    (with-redefs [sse/with-sse capture-sse]
      (let [htmls (sent-html (call deps (datastar-req {:post true :member false :group false :event false})))
            hits-html (:html (first (filter #(= "#connect-hits" (get-in % [:opts :selector])) htmls)))]
        (is (str/includes? hits-html "A Post"))
        (is (not (str/includes? hits-html "Ada")))
        (is (not (str/includes? hits-html "Bob's Club")))))))

(deftest connect-datastar-no-types-shows-all
  (let [deps (stub-deps :search-repo
                        (memory/new-memory-search-repository
                         [{:type :member :id 1 :title "Ada" :description "x" :activity-at 2}
                          {:type :post :id 4 :title "A Post" :description "z" :activity-at 1}]))]
    (with-redefs [sse/with-sse capture-sse]
      (let [htmls (sent-html (call deps (datastar-req {:post false :member false :group false :event false})))
            hits-html (:html (first (filter #(= "#connect-hits" (get-in % [:opts :selector])) htmls)))]
        (is (str/includes? hits-html "Ada"))
        (is (str/includes? hits-html "A Post"))))))

(deftest connect-datastar-geo-off-skips-infer
  (let [deps (stub-deps
              :infer-location (constantly {:lat 59.44 :lng 24.75 :label "Tallinn" :radius-km 100})
              :search-repo (memory/new-memory-search-repository
                            [{:type :member :id 1 :title "Nearby Ada" :description "x"
                              :lat 59.44 :lng 24.75 :activity-at 2}
                             {:type :member :id 2 :title "Far Bob" :description "y"
                              :lat 40.7 :lng -74.0 :activity-at 1}]))]
    (with-redefs [sse/with-sse capture-sse]
      (let [htmls (sent-html (call deps (datastar-req {:geo "off"})))
            hits-html (:html (first (filter #(= "#connect-hits" (get-in % [:opts :selector])) htmls)))
            loc-html (:html (first (filter #(= "#connect-location" (get-in % [:opts :selector])) htmls)))]
        (is (str/includes? hits-html "Far Bob"))
        (is (str/includes? loc-html "Near me"))))))

(deftest connect-datastar-inner-resets-offset
  (let [deps (stub-deps)]
    (with-redefs [sse/with-sse capture-sse]
      (let [htmls (sent-html (call deps (datastar-req {:offset 20 :limit 10})))
            hits-html (:html (first (filter #(= "#connect-hits" (get-in % [:opts :selector])) htmls)))]
        (is (str/includes? hits-html "Ada"))))))

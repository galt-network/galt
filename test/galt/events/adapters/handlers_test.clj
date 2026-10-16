(ns galt.events.adapters.handlers-test
  (:require
   [clojure.test :refer [deftest is]]
   [galt.events.adapters.handlers :as handlers]
   [galt.events.domain.event-repository :refer [EventRepository]]
   [java-time.api :as jt]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.mock.request :as mock]))

(defn- stub-event-repo []
  (reify EventRepository
    (add-event [_ event] event)
    (get-event [_ _] nil)
    (update-event [_ _ _] nil)
    (delete-event [_ _] nil)
    (rsvp-event [_ _ _] nil)
    (list-events [_ _] [])
    (count-events [_] 0)
    (list-recent-events [_ _] [])))

(defn- post-event [params]
  (let [captured (atom nil)
        deps {:render identity
              :layout identity
              :gen-uuid (constantly #uuid "01900000-0000-7000-8000-000000000001")
              :add-event-use-case (fn [command]
                                    (reset! captured (:event command))
                                    [:ok nil])
              :event-repo (stub-event-repo)}
        handler (-> (partial handlers/create deps)
                    wrap-keyword-params
                    wrap-params)]
    {:response (handler (mock/request :post "/events" params))
     :event @captured}))

(defn- saved-event [params]
  (:event (post-event params)))

(deftest a-blank-publish-at-is-left-to-the-database
  (let [event (saved-event {"name" "Meetup"
                            "description" "Talk"
                            "start-time" "2026-11-01T18:00"
                            "publish-at" ""})]
    (is (not (contains? event :publish-at)))))

(deftest a-blank-end-time-stays-empty
  (let [event (saved-event {"name" "Meetup"
                            "description" "Talk"
                            "start-time" "2026-11-01T18:00"
                            "end-time" ""})]
    (is (nil? (:end-time event)))))

(deftest a-publish-at-from-the-form-is-parsed
  (let [event (saved-event {"name" "Meetup"
                            "description" "Talk"
                            "start-time" "2026-11-01T18:00"
                            "publish-at" "2026-11-01T09:30"})]
    (is (= (jt/local-date-time 2026 11 1 9 30) (:publish-at event)))))

(deftest creating-an-event-responds-created
  (is (= 201 (:status (:response (post-event {"name" "Meetup"
                                              "description" "Talk"
                                              "start-time" "2026-11-01T18:00"}))))))

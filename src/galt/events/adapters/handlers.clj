(ns galt.events.adapters.handlers
  (:require
   [galt.core.adapters.link-generator :refer [link-for-route]]
   [galt.core.adapters.sse-helpers :refer [with-sse]]
   [galt.core.adapters.time-helpers :as th]
   [galt.events.adapters.presentation.list-events :as presentation.list-events]
   [galt.events.adapters.presentation.new-event :as presentation.new-event]
   [galt.events.domain.event-repository :as er]
   [ring.util.http-status :as http-status]
   [galt.core.infrastructure.web.helpers :refer [get-signals]]
   [starfederation.datastar.clojure.api :refer [datastar-request?]]))

(defn new-event
  [{:keys [render layout]} req]
  {:status http-status/ok :body (-> {} presentation.new-event/present layout render)})



(defn create
  [{:keys [render gen-uuid add-event-use-case event-repo layout]} req]
  (let [params (get req :params)
        event {:id (gen-uuid)
               :name (:name params)
               :description (:description params)
               :author-id (get-in req [:session :member-id])
               :start-time (th/parse-form-datetime (:start-time params))
               :end-time (th/parse-form-datetime (:end-time params))
               :publish-at (th/parse-form-datetime (:publish-at params))
               :location-id (when (seq (:location-id params)) (:location-id params))
               :type (:type params)}
        [status result] (add-event-use-case {:event event})
        events (er/list-events event-repo {})]
    {:status http-status/created :body (-> {:events events} presentation.list-events/present layout render)}))


(defn list-events
  [{:keys [render list-events-use-case layout]} req]
  (if (datastar-request? req)
    (with-sse req
      (fn [send!]
        (let [signals (get-signals req)
              patch-mode (get-in req [:params :patch-mode])
              limit (if (= patch-mode "inner") 5 (get signals :limit 5))
              offset (if (= patch-mode "inner") 0 (get signals :offset 0))
              next-offset (+ limit offset)
              {:keys [start-time end-time]} (th/period-range (keyword (:period signals)))
              type (:type signals)
              command {:limit limit :offset offset :from-date start-time :to-date end-time :type type}
              [status result] (list-events-use-case command)]
          (send! :html (map presentation.list-events/event-card result) {:selector "#event-cards"
                                                                         :patch-mode patch-mode})
          (send! :signals {:offset next-offset :limit limit}))))
    (let [limit 5
          offset 0
          [status result] (list-events-use-case {:limit limit :offset offset})
          model {:new-event-href (link-for-route req :events/new)
                 :events result
                 ; :initial-signals "{offset: 0, limit: 5}"
                 :offset offset
                 :limit limit
                 }]
      {:status http-status/ok
       :body (-> model presentation.list-events/present layout render)})))

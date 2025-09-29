(ns galt.events.adapters.handlers
  (:require
    [ring.util.http-status :as http-status]
    [galt.core.adapters.link-generator :refer [link-for-route]]
    [galt.events.domain.event-repository :as er]
    [galt.core.adapters.time-helpers :as th]
    [galt.events.adapters.presentation.new-event :as presentation.new-event]
    [galt.events.adapters.presentation.list-events :as presentation.list-events]))

(defn new-event
  [{:keys [render layout]} req]
  {:status http-status/ok :body (-> {} presentation.new-event/present layout render)})


(def last-req (atom nil))
(seq (get-in @last-req [:params :location-id]))

(defn create
  [{:keys [render gen-uuid add-event-use-case event-repo layout]} req]
  (reset! last-req req)
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
  [{:keys [render event-repo layout]} req]
  (let [events (er/list-events event-repo {})
        model {:new-event-href (link-for-route req :events/new)
               :events events}]
    (println ">>> EVENTS" events)
    {:status http-status/ok
     :body (-> model presentation.list-events/present layout render)}))

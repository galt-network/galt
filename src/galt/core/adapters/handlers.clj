(ns galt.core.adapters.handlers
  (:require
   [galt.core.adapters.link-generator :refer [link-for-route]]
   [galt.core.adapters.sse-helpers :refer [with-sse]]
   [galt.core.infrastructure.web.sse-connection-store :refer [add-connection
                                                               remove-connection]]
   [galt.core.views.landing-page :as landing-page]
   [starfederation.datastar.clojure.adapter.http-kit :refer [->sse-response
                                                              on-close
                                                              on-open]]

   [starfederation.datastar.clojure.api :as d*]))

(defn view-landing
  [{:keys [render layout landing-page-use-case layout-model] :as _deps} req]
  (let [user-id (get-in req [:session :user-id])
        member-id (get-in req [:session :member-id])
        [status result] (landing-page-use-case {:member-id member-id})
        with-links (fn [items route]
                     (map #(assoc % :href (link-for-route req route {:id (:id %)}))
                          items))]
    (case status
      :ok
      (let [model (merge result
                         {:authenticated? (some? user-id)
                          :user (get-in layout-model [:navbar :user])
                          :groups-href (link-for-route req :groups)
                          :members-href (link-for-route req :members)
                          :events-href (link-for-route req :events)
                          :map-href "/world-map"
                          :login-href (link-for-route req :members/login)
                          :recent-groups (with-links (:recent-groups result) :groups/by-id)
                          :recent-posts (with-links (:recent-posts result) :posts/by-id)
                          :recent-events (with-links (:recent-events result) :events/by-id)
                          :my-groups (when member-id
                                       (with-links (:my-groups result) :groups/by-id))})]
        {:status 200
         :body (render (layout {:content (landing-page/page model)}))})
      {:status 500
       :body (render (layout {:content (landing-page/page {})}))})))

(defn serve-file
  [{:keys [file-storage]} req]
  (let [file-path (get-in req [:path-params :path])]
    ((:content-response file-storage) file-path)))

(defn store-file
  [{:keys [file-storage]} req]
  (let [uploaded-file (get-in req [:multipart-params "uploaded-file"])
        result ((:store-content file-storage) uploaded-file)]
    (if (d*/datastar-request? req)
      (with-sse req (fn [send!] (send! :signals {:uploaded-url (:url result)})))
      {:status 201 :body (:url result)})))

(defn datastar-sse
  [deps req]
  (let [session-id (get-in req [:cookies "ring-session" :value])
        connection-id (or (get-in req [:query-params "connection-id"]) session-id)]
    (->sse-response
      req
      {on-open (fn [sse]
                 (add-connection connection-id sse))
       on-close (fn [sse _status]
                  (remove-connection connection-id))})))

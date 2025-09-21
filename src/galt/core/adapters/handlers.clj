(ns galt.core.adapters.handlers
  (:require
   [galt.core.adapters.sse-helpers :refer [with-sse]]
   [galt.core.infrastructure.web.sse-connection-store :refer [add-connection
                                                              remove-connection]]
   [galt.core.views.landing-page :as landing-page]
   [starfederation.datastar.clojure.adapter.http-kit :refer [->sse-response
                                                             on-close
                                                             on-open]]
   [starfederation.datastar.clojure.api :as d*]))

(defn view-landing
  [{:keys [render layout] :as _deps} _req]
  {:status 200
   :body (render (layout {:content (landing-page/page nil)}))})

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
  (let [connection-id (get-in req [:query-params "connection-id"])]
    (->sse-response
      req
      {on-open (fn [sse]
                 (println ">>> datastar-sse on-open" sse connection-id)
                 (add-connection connection-id sse))
       on-close (fn [sse]
                  (println ">>> datastar-sse on-close" sse connection-id)
                  (remove-connection connection-id))})))

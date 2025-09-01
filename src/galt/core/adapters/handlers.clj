(ns galt.core.adapters.handlers
  (:require
    [galt.core.views.landing-page :as landing-page]
    [galt.core.infrastructure.web.helpers :refer [get-signals]]
    [galt.core.adapters.sse-helpers :refer [with-sse]]
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
        element-selector (get-in req [:params "upload-target-element-selector"])
        result ((:store-content file-storage) uploaded-file)]

    (if (d*/datastar-request? req)
            (with-sse
              req
              (fn [send!]
                (send! :signals {:uploaded-url (:url result)})
                (when element-selector
                  (send! :html [(keyword element-selector) {:src (:url result)}]))))
            {:status 200 :body ""})))

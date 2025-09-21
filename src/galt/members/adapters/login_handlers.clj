(ns galt.members.adapters.login-handlers
  (:require
   [galt.core.adapters.link-generator :refer [link-for-route]]
   [galt.core.adapters.sse-helpers :refer [send! with-sse]]
   [galt.core.adapters.url-helpers :refer [decode-url-encoded]]
   [galt.core.infrastructure.web.helpers :refer [->json]]
   [galt.core.infrastructure.web.sse-connection-store :refer [get-connection]]
   [galt.core.views.datastar-helpers :refer [d*-backend-action]]
   [galt.members.adapters.presentation.show-login :as presentation.show-login]
   [galt.members.adapters.view-models :refer [login-result-view-model]]))

(defn show-login
  [{:keys [layout start-lnurl-login-use-case render]} req]
  (let [message (decode-url-encoded (get-in req [:query-params "message"]))
        session-id (get-in req [:cookies "ring-session" :value])
        callback-path (link-for-route req :members.login/lnurl-auth)
        datastar-action (d*-backend-action "/datastar-sse" :post {:connection-id session-id})
        [_status lnurl] (start-lnurl-login-use-case {:session-id session-id
                                                     :callback-path callback-path})
        model {:message message
               :lnurl lnurl
               :datastar-action datastar-action}]
    {:status 200
     :body (-> model presentation.show-login/present layout render)}))

(defn lnurl-auth-callback
  [{:keys [complete-lnurl-login-use-case] :as deps} req]
  (let [params (:params req)
        command {:challenge (:k1 params)
                 :signed-challenge (:sig params)
                 :user-pub-key (:key params)
                 :session-id (:galt-session-id params)}
        session-id (:galt-session-id params)
        [status result] (complete-lnurl-login-use-case command)
        model (login-result-view-model status result)]
    (send! (get-connection session-id) :html (presentation.show-login/login-result model))
    (case status
      :ok {:status 200 :body (->json {:status "OK"})}
      :error {:status 200 :body (->json {:status "ERROR" :reason result})})))

(defn logout
  [{:keys [app-container delete-session] :as deps} req]
  (with-sse
    req
    (fn [send!]
      (let [session-key (:session/key req)
            req-logged-out (assoc-in req [:session] nil)
            model-after-logging-out ((:update-layout-model deps) req-logged-out)]
        (delete-session session-key)
        (send! :html (app-container (assoc model-after-logging-out
                                           :content [:h1 "You have been logged out"])))))))

(ns galt.members.adapters.login-handlers
  (:require
   [galt.core.adapters.link-generator :refer [link-for-route]]
   [galt.core.adapters.sse-helpers :refer [send! with-sse]]
   [galt.core.adapters.url-helpers :refer [decode-url-encoded]]
   [galt.core.infrastructure.web.helpers :refer [->json]]
   [galt.core.infrastructure.web.sse-connection-store :refer [get-connection]]
   [galt.core.views.datastar-helpers :refer [d*-backend-action]]
   [galt.members.adapters.presentation.show-login :as presentation.show-login]
   [galt.members.adapters.view-models :refer [login-result-view-model]]
   [ring.middleware.session]
   [starfederation.datastar.clojure.api :refer [datastar-request?]]))

(defn show-login
  [{:keys [layout start-lnurl-login-use-case render gen-uuid]} req]
  (if (datastar-request? req)
    (let [message (decode-url-encoded (get-in req [:query-params "message"]))
          session-id (get req :session/key)
          callback-path (link-for-route req :members.login/lnurl-auth)
          datastar-action (d*-backend-action "/datastar-sse" :post {:connection-id session-id})
          [_status lnurl] (start-lnurl-login-use-case {:session-id session-id
                                                       :callback-path callback-path})
          model {:message message
                 :lnurl lnurl
                 :datastar-action datastar-action}]
      (with-sse req
        (fn [send!]
          (send! :html (presentation.show-login/qr-code model)
                 {:selector "#qr-code"}))))
    (let [logged-in? (get-in req [:session :user-id])
          content (if logged-in?
                    (presentation.show-login/already-logged-in {})
                    (presentation.show-login/present {}))]
      {:status 200
       :session {:login-start (System/currentTimeMillis)}
       :body (-> content layout render)})))

(defn lnurl-auth-callback
  [{:keys [app-container complete-lnurl-login-use-case] :as deps} req]
  (let [params (:params req)
        command {:challenge (:k1 params)
                 :signed-challenge (:sig params)
                 :user-pub-key (:key params)
                 :session-id (:galt-session-id params)}
        session-id (:galt-session-id params)
        [status result] (complete-lnurl-login-use-case command)
        model (login-result-view-model status result)
        req-logged-in (assoc req :session (:session result))
        model-after-logging-in ((:update-layout-model deps) req-logged-in)]
    (send! (get-connection session-id)
           :html (app-container (assoc model-after-logging-in
                                       :content
                                       (presentation.show-login/login-result model))))
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
            result-model {:message-class "is-success"
                          :message-header "Logged out"
                          :message-body "You have successfully logged out"}
            model-after-logging-out ((:update-layout-model deps) req-logged-out)]
        (delete-session session-key)
        (send! :html (app-container (assoc model-after-logging-out
                                           :content (presentation.show-login/login-result result-model))))))))

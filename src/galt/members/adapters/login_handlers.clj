(ns galt.members.adapters.login-handlers
  (:require
    [galt.members.adapters.presentation.qr-code :as presentation.qr-code]
    [galt.members.adapters.views :as views]
    [galt.core.adapters.sse-helpers :refer [close! send! with-sse]]
    [galt.core.adapters.url-helpers :refer [decode-url-encoded]]
    [galt.members.adapters.view-models :as view-models]
    [starfederation.datastar.clojure.adapter.http-kit :refer [->sse-response on-open]]))

(defn show-login
  [{:keys [layout render]} req]
  (let [session-id (str (random-uuid))
        message (decode-url-encoded (get-in req [:query-params "message"]))
        updated-session (merge (:session req) {:show-login-id session-id})
        content (views/login-form {:message message})]
    {:session updated-session
     :body (render (layout content))
     :status 200}))

; TODO Refactor to DB or memory store to be injected or read from :session
(defonce k1-store (atom {}))

(defn- add-to-lnurl-session
  [session-id props]
  (swap! k1-store assoc-in [session-id] props))

(defn- get-lnurl-session
  [session-id]
  (@k1-store session-id))

(defn do-login
  [{:keys [generate-lnurl]} req]
  (->sse-response
    req
    {on-open
     (fn [sse]
      (let [session-id (get-in req [:cookies "ring-session" :value])
            lnurl-result (generate-lnurl {:galt-session-id session-id})
            {:keys [lnurl k1-hex]} lnurl-result
            ten-minutes (* 1000 60 10)
            in-ten-minutes (+ (System/currentTimeMillis) ten-minutes)]
        (send! sse :html [:div#login-action-description
                          "Scan the QR code with your Bitcoin Lightning wallet"])
        (send! sse :html [:div#login-area
                          [:div.level
                           [:div.level-item (presentation.qr-code/qr-code-img lnurl)]]])
        (add-to-lnurl-session k1-hex {:k1 k1-hex :connection sse :expires-at in-ten-minutes})))}))

(defn lnurl-auth-callback
  [{:keys [create-lightning-user-use-case] :as deps} req]
  (let [params (get req :params) ; TODO verify presence of params with spec?
        ->json (:->json deps)
        lnurl-session (get-lnurl-session (:k1 params))
        login-params {:our-k1 (:k1 lnurl-session)
                      :user-k1 (:k1 params)
                      :k1-expires-at (:expires-at lnurl-session)
                      :user-pub-key (:key params)
                      :signed-challenge (:sig params)}
        [status result] (create-lightning-user-use-case login-params)
        ; TODO: see why logging-in doesn't work in staging
        logged-in-user-id (get-in result [:user :id])
        logged-in-member-id (get-in result [:member :id])
        original-session-id (:galt-session-id params)
        view-model ((:update-layout-model deps)
                    (update-in req [:session] assoc
                               :user-id logged-in-user-id
                               :member-id logged-in-member-id))
        login-result-model (view-models/login-result-view-model status result)]
    (case status
      :ok (do
            (-> (lnurl-session :connection)
                (send! ,,, :html [:div {:id "login-area"} (views/login-result-message login-result-model)])
                (send! ,,, :html ((:navbar deps) (:navbar view-model)))
                (close! ,,,))
            {:status 200
             :body (->json {:status "OK"})
             :headers {"Content-Type" "application/json"}
             :update-related-session [original-session-id {:user-id logged-in-user-id
                                                           :member-id logged-in-member-id}]})
      :error {:status 200
              :headers {"Content-Type" "application/json"}
              :body (->json {:status "ERROR" :reason (:message result)})})))

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

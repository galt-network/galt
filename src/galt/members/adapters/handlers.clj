(ns galt.members.adapters.handlers
  (:require
    [galt.core.adapters.sse-helpers :refer [with-sse send! close!]]
    [galt.core.adapters.link-generator :refer [link-for-route]]
    [starfederation.datastar.clojure.adapter.http-kit :refer [->sse-response on-open]]
    [galt.members.use-cases.create-lightning-user :refer [new-create-lightning-user]]
    [galt.members.domain.user-repository :refer [find-user-by-id]]
    [galt.members.adapters.views :as views]
    [galt.members.adapters.presentation.profile :as profile]
    [galt.members.adapters.view-models :as view-models]
    [reitit.core]))

(defn show-members-list
  [{:keys [render layout] :as deps} req]
  (let [model-deps (merge
                     {:link-for-route (partial link-for-route req)}
                     (select-keys deps [:user-repo :group-repo]))
        content (views/members-list (view-models/members-model model-deps req))]
    {:status 200 :body (render (layout content))}))

(defn show-my-profile
  [{:keys [render user-repo layout]} req]
  (let [user-id (get-in req [:session :user-id])
        user (find-user-by-id user-repo user-id)
        _ (println ">>> show-my-profile user:" user)
        model {:member? false :user user :new-invitation-url (link-for-route req :invitations/new)}
        content (profile/present model)]
    {:status 200
     :body (render (layout content))}))

(defn show-profile
  [{:keys [render user-repo layout]} req]
  (let [user-id (parse-uuid (get-in req [:path-params :id]))
        user (find-user-by-id user-repo user-id)
        content [:div.content [:strong (:users/name user)]]]
    {:status 200
     :body (render (layout content))}))

(defn edit-my-profile
  [deps req]
  (let []))

(defn show-login
  [{:keys [layout render]} req]
  (let [session-id (str (random-uuid))
        updated-session (merge (:session req) {:show-login-id session-id})
        content (views/login-form nil)]
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
                           [:div.level-item (views/qr-code-img lnurl)]]])
        (add-to-lnurl-session k1-hex {:k1 k1-hex :connection sse :expires-at in-ten-minutes})))}))

(defn lnurl-auth-callback
  [deps req]
  (let [params (get req :params) ; TODO verify presence of params with spec?
        ->json (:->json deps)
        lnurl-session (get-lnurl-session (:k1 params))
        login-params {:our-k1 (:k1 lnurl-session)
                      :user-k1 (:k1 params)
                      :k1-expires-at (:expires-at lnurl-session)
                      :user-pub-key (:key params)
                      :signed-challenge (:sig params)}
        [status result] (new-create-lightning-user deps login-params)
        ; TODO: see why logging-in doesn't work in staging
        logged-in-user-id (get-in result [:user :users/id])
        original-session-id (:galt-session-id params)
        view-model ((:update-layout-model deps) (assoc-in req [:session :user-id] logged-in-user-id))]
    (case status
      :ok (do
            (-> (lnurl-session :connection)
                (send! ,,, :html [:div {:id "login-area"} (views/login-result-message status result)])
                (send! ,,, :html ((:navbar deps) (:navbar view-model)))
                (close! ,,,))
            {:status 200
             :body (->json {:status "OK"})
             :headers {"Content-Type" "application/json"}
             :update-related-session [original-session-id {:user-id logged-in-user-id}]})
      :error {:status 200
              :headers {"Content-Type" "application/json"}
              :body (->json {:status "ERROR" :reason (:message result)})})))

(defn logout
  [{:keys [app-container galt-session] :as deps} req]
  (with-sse
    req
    (fn [send!]
      (let [session-key (:session/key req)
            _ (swap! galt-session assoc-in [session-key :user-id] nil)
            req-logged-out (assoc-in req [:session :user-id] nil)
            model-after-logging-out ((:update-layout-model deps) req-logged-out)]
        (send! :html (app-container (assoc model-after-logging-out :content [:h1 "You have been logged out"])))))))

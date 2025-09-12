(ns galt.members.adapters.handlers
  (:require
   [galt.core.adapters.link-generator :refer [link-for-route]]
   [galt.core.adapters.sse-helpers :refer [close! send! with-sse]]
   [galt.core.adapters.url-helpers :refer [decode-url-encoded]]
   [galt.core.infrastructure.web.helpers :refer [get-signals]]
   [galt.core.views.components.dropdown-search :refer [dropdown-search-menu
                                                       id-element-name
                                                       show-results-signal-name]]
   [galt.members.adapters.presentation.members-list :as presentation.members-list]
   [galt.members.adapters.presentation.non-member-profile :as non-member-profile]
   [galt.members.adapters.presentation.profile :as presentation.profile]
   [galt.members.adapters.view-models :as view-models]
   [galt.members.adapters.views :as views]
   [galt.members.domain.member-repository :as mr]
   [reitit.core]
   [starfederation.datastar.clojure.adapter.http-kit :refer [->sse-response
                                                             on-open]]))

(defn show-members-list
  [{:keys [render search-members-use-case layout] :as deps} req]
  (let [[status result] (search-members-use-case {:query ""})
        model (view-models/members-search-view-model result (partial link-for-route req))
        content (presentation.members-list/present model)]
    {:status 200 :body (render (layout content))}))

(defn show-my-profile
  [{:keys [render layout member-repo show-profile-use-case]} req]
  (let [user-id (get-in req [:session :user-id])
        member (mr/find-member-by-user-id member-repo user-id)
        [status result] (show-profile-use-case {:member-id (:id member)})]
    (case status
      :ok {:status 200
           :body (-> result
                     view-models/profile-view-model
                     presentation.profile/present
                     layout
                     render)}
      :error {:status 400
              :body (-> (link-for-route req :invitations/new-request)
                        non-member-profile/present
                        layout
                        render)})))

(defn show-profile
  [{:keys [render layout show-profile-use-case]} req]
  (let [member-id (parse-uuid (get-in req [:path-params :id]))
        [status result] (show-profile-use-case {:member-id member-id})]
    (case status
      :ok {:status 200
           :body (-> result
                     view-models/profile-view-model
                     presentation.profile/present
                     layout
                     render)}
      :error {:status 400
              :body (-> [result]
                        presentation.profile/present-error
                        layout
                        render)})))

(defn edit-my-profile
  [deps req]
  (let []))

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
                           [:div.level-item (views/qr-code-img lnurl)]]])
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
        view-model ((:update-layout-model deps) (update-in req [:session] assoc
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
  [{:keys [app-container galt-session] :as deps} req]
  (with-sse
    req
    (fn [send!]
      (let [session-key (:session/key req)
            _ (swap! galt-session assoc-in [session-key :user-id] nil)
            req-logged-out (assoc-in req [:session :user-id] nil)
            model-after-logging-out ((:update-layout-model deps) req-logged-out)]
        (send! :html (app-container (assoc model-after-logging-out
                                           :content [:h1 "You have been logged out"])))))))

; (defn search-members
;   [{:keys [search-members-use-case]} req]
;   (with-sse
;     req
;     (fn [send!]
;       (let [query (:query (get-signals req))
;             [status result] (search-members-use-case {:query query})
;             panel-items (view-models/members-search-view-model result (partial link-for-route req))]
;         (send! :html (presentation.members-list/search-results
;                        (map presentation.members-list/panel-item panel-items)))))))


(defn search-members
  [{:keys [member-repo]} req]
  (let [signals (get-signals req)
        action (get-in req [:params :action])
        _ (println ">>> search-members signals" signals " | action:" action)
        search-signal-name (get-in req [:params :search-signal-name])
        extra-signal-name (get-in req [:params :extra-signal-name])
        group-id (some-> (get signals (keyword extra-signal-name))
                             parse-uuid)
        query (get signals (keyword search-signal-name))
        fuzzy-find-groups (fn [q] (->> (mr/find-members-by-name member-repo query group-id)
                                       (map (fn [m] {:value (:name m) :id (:id m)}) ,,,)))]
    (with-sse req
      (fn [send!]
        (case action
          "search"
          (do
            (send! :html (dropdown-search-menu search-signal-name "/members/search" (fuzzy-find-groups query)))
            (send! :signals {(show-results-signal-name search-signal-name) true}))
          "choose"
          (let [search-input-signal-name (get-in req [:params :name])
                search-input-signal-value (get-in req [:params :value])
                id-element-value (get-in req [:params :id])]
            (send! :signals {search-input-signal-name search-input-signal-value
                             (id-element-name search-input-signal-name) id-element-value
                             (show-results-signal-name search-input-signal-name) false})))))))

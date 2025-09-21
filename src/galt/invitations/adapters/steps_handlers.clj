(ns galt.invitations.adapters.steps-handlers
  (:require
   [clojure.string :as str]
   [galt.core.adapters.link-generator :refer [link-for-route]]
   [galt.core.adapters.sse-helpers :refer [with-sse]]
   [galt.core.infrastructure.web.helpers :refer [->json read-json]]
   [galt.core.views.components :refer [message]]
   [galt.core.views.datastar-helpers :refer [d*-backend-action]]
   [galt.invitations.adapters.presentation.steps :as steps]))

(defn steps-model [current-step href-for-step]
  (let [step-names [:start :login :payment :complete]
        route-name-base :invitations.steps
        step-route-names (map (fn [step] (keyword (name route-name-base) (name step))) step-names)
        ->step (fn [route-name] {:name (str/capitalize (name route-name))
                                 :active? (= current-step route-name)
                                 :href (href-for-step route-name)})]
    (map ->step step-route-names)))

(defn href-for-step [router id step]
  (link-for-route router step {:id id}))

(defn steps
  [{:keys [render layout]} req]
  (let [invitation-id (get-in req [:path-params :id])
        steps (steps-model :invitations.steps/start (partial href-for-step req invitation-id))]
    {:status 200 :body (-> steps steps/steps-progress layout render)}))

(defn show-start
  [{:keys [render layout]} req]
  (let [invitation-id (get-in req [:path-params :id])
        href-for-step (partial href-for-step req invitation-id)
        steps (steps-model :invitations.steps/start href-for-step)
        model {:steps steps
               :next-step (href-for-step :invitations.steps/login)}]
    {:status 200 :body (-> model steps/start layout render)}))

(defn show-login
  [{:keys [render start-lnurl-login-use-case layout]} req]
  (let [invitation-id (get-in req [:path-params :id])
        steps (steps-model :invitations.steps/login (partial href-for-step req invitation-id))
        session-id (get-in req [:cookies "ring-session" :value])
        callback-path (link-for-route req :invitations.steps/lnurl-callback {:id invitation-id})
        [status lnurl] (start-lnurl-login-use-case {:session-id session-id
                                                    :callback-path callback-path})
        status-url (link-for-route req :invitations.steps/login-status {:id invitation-id})
        model {:steps steps
               :lnurl lnurl
               :status-poll-action (d*-backend-action status-url)}]
    {:status 200 :body (-> model steps/login layout render)}))

(defn login-status
  [{:keys [watch-lnurl-login-use-case]} req]
  (let [invitation-id (get-in req [:path-params :id])
        [status result] (watch-lnurl-login-use-case {:session-id (:session/key req)})
        login-status (:status result)
        result-message (:message result)
        next-step-url (link-for-route req :invitations.steps/payment {:id invitation-id})]
    (println ">>> LOGIN STATUS" status result)
    (with-sse req
      (fn [send!]
        (case login-status
          :logged-in
          (do
            (send! :html (steps/login-area (steps/login-success {:next-step next-step-url})))
            #_(send! :to-url next-step-url))
          :expired
          (send! :html (steps/login-area (message {:title (name login-status)
                                                   :content result-message
                                                   :type :error}))))))))

(defn lnurl-callback
  [{:keys [complete-lnurl-login-use-case]} req]
  (let [params (:params req)
        command {:challenge (:k1 params)
                 :signed-challenge (:sig params)
                 :user-pub-key (:key params)
                 :session-id (:galt-session-id params)}
        [status result] (complete-lnurl-login-use-case command)]
    (println ">>> login-callback" status result)
    (case status
      :ok {:status 200 :body (->json {:status "OK"})}
      :error {:status 200 :body (->json {:status "ERROR" :reason result})})))


(defn show-payment
  [{:keys [layout render]} req]
  (let [model {}]
  {:status 200 :body (-> model steps/payment layout render)}))

(defn show-complete
  [deps req]
  {:status 200 :body "Complete"})


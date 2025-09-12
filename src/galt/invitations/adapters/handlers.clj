(ns galt.invitations.adapters.handlers
  (:require
   [galt.core.adapters.link-generator :refer [link-for-route]]
   [java-time.api :as jt]
   [galt.invitations.domain.invitation-repository :as ir]
   [galt.invitations.adapters.presentation.invitation-request :as invitation-request]
   [galt.invitations.adapters.presentation.new-invitation :as new-invitation]
   [galt.invitations.adapters.presentation.invitation :as invitation-page]
   [galt.invitations.adapters.presentation.invitations-dashboard :as dashboard]
   ))

(defn new-invitation
  [{:keys [render layout]} req]
  (let [model {:form (get-in req [:params])
               :form-action (link-for-route req :invitations/new)
               :expiration-min "2025-09-12"
               :expiration-max "2026-01-01"}]
    {:status 200 :body (-> model new-invitation/present layout render)}))

(defn create-invitation
  [{:keys [create-invitation-use-case gen-uuid]} req]
  (let [params (get req :params)
        invitation {:id (gen-uuid)
                    :inviting-member-id (get-in req [:session :member-id])
                    :target-group-id (when (:group-name-id params) (parse-uuid (:group-name-id params)))
                    :content (:content params)
                    :expires-at (jt/local-date (:expires-at params))
                    :max-usages (Integer/parseInt (:max-usages params))}
        [status result] (create-invitation-use-case {:invitation invitation})]
    (println ">>> NEW-INVITATION result" [status result])
    {:status 303 :headers {"Location" (link-for-route req :invitations/by-id {:id (:id result)})}}))

(defn show-invitation
  [{:keys [render layout invitation-repo]} req]
  (let [invitation (ir/invitation-by-id invitation-repo (parse-uuid (get-in req [:path-params :id])))
        model {:invitation invitation}]
    {:status 200 :body (render (layout (invitation-page/present model)))}))

(defn new-invitation-request
  [{:keys [render layout]} req]
  (let [
        model {}]
    ; TODO: implement fuzzy group and member search (members need repository)
    ;       This handler function needs to distinguish between SSE and normal
    ;       In the SSE case it'll send the search dropdown hiccup vectors
    {:status 200 :body (render (layout {:content (invitation-request/present model)
                                        :page-title "New Invitation"}))}))
(defn create-invitation-request
  [{:keys [create-invitation-request-use-case render layout]} req]
  (let [from-user-id (get-in req [:session :user-id])
        to-member-id (get-in req [:params :member-name-id])
        to-group-id (get-in req [:params :group-name-id])
        content (get-in req [:params :content])
        use-case-params {:from-user-id from-user-id
                         :to-member-id (parse-uuid to-member-id)
                         :to-group-id (parse-uuid to-group-id)
                         :email (get-in req [:params :email])
                         :content content}
        [status result] (create-invitation-request-use-case use-case-params)]
    (case status
      :error {:status 500 :body (-> {:form (:params req) :errors result}
                                    invitation-request/present
                                    layout
                                    render)}
      :ok {:status 303 :headers {"Location" (link-for-route req :invitations)}})))

(defn list-invitations
  [{:keys [render layout]} req]
  (let [model {:requests
               [{:requesting-user "Charlie Kirk" :to-member "Sarah Connor" :to-group "San Blas Ancap"}]
               :invitations
               [{:inviter "Charlie Brown" :invited-user "Alice Alba" :created-at "2 months ago (2025-07-01)" :expires-at "In 3 weeks (2025-09-30)" :max-usages 1 :current-usages 0}]}]
    {:status 200 :body (render (layout (dashboard/present model))) }))

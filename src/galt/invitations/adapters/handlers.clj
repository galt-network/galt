(ns galt.invitations.adapters.handlers
  (:require
   [galt.core.adapters.link-generator :refer [link-for-route]]
   [java-time.api :as jt]
   [galt.invitations.domain.invitation-repository :as ir]
   [galt.invitations.adapters.presentation.invitation-request :as invitation-request]
   [galt.invitations.adapters.presentation.new-invitation :as new-invitation]
   [galt.invitations.adapters.presentation.invitation :as invitation-page]
   [galt.invitations.adapters.presentation.invitations-dashboard :as dashboard]
    ; Groups search deps
    [galt.core.adapters.sse-helpers :refer [with-sse]]
    [galt.groups.domain.group-repository :as gr]
    [galt.core.infrastructure.web.helpers :refer [get-signals]]
    [galt.core.views.components.dropdown-search :refer [dropdown-search-menu
                                                        id-element-name
                                                        show-results-signal-name]]
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
  [{:keys [render invitation-dashboard-use-case layout]} req]
  (let [[status result] (invitation-dashboard-use-case {:member-id (get-in req [:session :member-id])})
        active (map #(assoc % :href (link-for-route req :invitations/by-id {:id (:id %)})) (:active result))
        model {:active active
               :inactive (:inactive result)}]
    {:status 200 :body (render (layout (dashboard/present model))) }))


(defn search-groups
  [{:keys [group-repo]} req]
  (let [signals (get-signals req)
        action (get-in req [:params :action])
        search-signal-name (get-in req [:params :search-signal-name])
        extra-signal-name (get-in req [:params :extra-signal-name])
        member-id (some-> (get signals (keyword extra-signal-name))
                             parse-uuid)
        query (get signals (keyword search-signal-name))
        fuzzy-find-groups (fn [q] (->> (gr/list-groups group-repo {:query q :member-id member-id})
                                       (map (fn [g] {:value (:name g) :id (:id g)}) ,,,)))]
    (with-sse req
      (fn [send!]
        (case action
          "search"
          (do
            (send! :html (dropdown-search-menu search-signal-name "/groups/search" (fuzzy-find-groups query)))
            (send! :signals {(show-results-signal-name search-signal-name) true}))
          "choose"
          (let [search-input-signal-name (get-in req [:params :name])
                search-input-signal-value (get-in req [:params :value])
                id-element-value (get-in req [:params :id])]
            (send! :signals {search-input-signal-name search-input-signal-value
                             (id-element-name search-input-signal-name) id-element-value
                             (show-results-signal-name search-input-signal-name) false})))))))

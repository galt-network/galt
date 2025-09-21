(ns galt.members.adapters.handlers
  (:require
   [galt.core.adapters.link-generator :refer [link-for-route]]
   [galt.core.adapters.sse-helpers :refer [with-sse]]
   [galt.core.adapters.url-helpers :refer [add-query-params]]
   [galt.core.infrastructure.web.helpers :refer [get-signals]]
   [galt.core.views.components.dropdown-search :refer [dropdown-search-menu
                                                       id-element-name
                                                       show-results-signal-name]]
   [galt.members.adapters.presentation.members-list :as presentation.members-list]
   [galt.members.adapters.presentation.new-member :as presentation.new-member]
   [galt.members.adapters.presentation.non-member-profile :as non-member-profile]
   [galt.members.adapters.presentation.profile :as presentation.profile]
   [galt.members.adapters.view-models :as view-models]
   [galt.members.domain.member-repository :as mr]
   [reitit.core]))

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
              :body (-> (add-query-params (link-for-route req :payments/new) {:type "galt-membership-payment"
                                                                              :return-to "/members/me"})
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

(defn new-member
  [{:keys [membership-payment-use-case render layout]} req]
  (let [invoice ()
        model {}]
    {:status 200 :body (-> model presentation.new-member/present layout render)}))

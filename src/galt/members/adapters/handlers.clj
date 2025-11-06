(ns galt.members.adapters.handlers
  (:require
   [clojure.core.match :refer [match]]
   [galt.core.adapters.link-generator :refer [link-for-route]]
   [galt.core.adapters.sse-helpers :refer [with-sse]]
   [galt.core.adapters.url-helpers :refer [add-query-params]]
   [galt.core.infrastructure.web.helpers :refer [get-signals]]
   [galt.core.views.components.dropdown-search :refer [dropdown-search-menu
                                                       id-element-name
                                                       show-results-signal-name]]
   [galt.groups.adapters.handlers :refer [head-tags-for-maps]]
   [galt.locations.domain.location-repository :as lr]
   [galt.members.adapters.presentation.edit-profile :as presentation.edit-profile]
   [galt.members.adapters.presentation.members-list :as presentation.members-list]
   [galt.members.adapters.presentation.new-member :as presentation.new-member]
   [galt.members.adapters.presentation.non-member-profile :as non-member-profile]
   [galt.members.adapters.presentation.profile :as presentation.profile]
   [galt.members.adapters.view-models :as view-models]
   [galt.members.domain.member-repository :as mr :refer [find-member-by-id]]
   [reitit.core]
   [starfederation.datastar.clojure.api :as d*]))

(defn show-members-list
  [{:keys [render search-members-use-case layout] :as deps} req]
  (let [active-tab (get-in req [:params :tab] "all")
        query (or (get-in req [:query-params "query"]) (get (get-signals req) :query))
        [status result] (search-members-use-case {:query query})
        model (view-models/members-search-view-model (merge result
                                                            {:active-tab active-tab
                                                             :link-for-route (partial link-for-route req)}))]
    (if (d*/datastar-request? req)
      (let []
        (with-sse req
          (fn [send!]
            (send! :html (presentation.members-list/search-results model)))))
      {:status 200
       :body (-> {:content (presentation.members-list/present model)
                  :head-tags (when (= "nearby" active-tab) head-tags-for-maps)}
                 layout
                 render)})))

(defn show-my-profile
  [{:keys [render layout show-profile-use-case]} req]
  (let [user-id (get-in req [:session :user-id])
        member-id (get-in req [:session :member-id])
        [status result] (show-profile-use-case {:member-id member-id :user-id user-id})
        edit-href (link-for-route req :members.me/edit)
        profile-content (fn []
                          (-> result
                              (assoc ,,, :edit-href edit-href)
                              view-models/profile-view-model
                              presentation.profile/present
                              layout
                              render))
        non-member-content (fn []
                             (-> (add-query-params (link-for-route req :payments/new)
                                                   {:type "galt-membership-payment" :return-to "/members/me"})
                                 non-member-profile/present
                                 layout
                                 render))]
    (match [status result]
      [:ok {:member nil}] {:status 302 :headers {"Location" (link-for-route req :members.me/edit)}}
      [:ok {:member _}] {:status 200 :body (profile-content)}
      [:error _] {:status 400 :body (non-member-content)})))

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
  [{:keys [render location-repo member-repo layout]} req]
  (let [logged-in-user-id (get-in req [:session :user-id])
        member (find-member-by-id member-repo logged-in-user-id)
        location (lr/find-location-by-id location-repo (:location-id member))
        [action-method action-target] (if member
                                        ["PUT" (link-for-route req :members/me)]
                                        ["POST" (link-for-route req :members)])
        model {:member member
               :location location
               :countries (lr/all-countries location-repo) ; TODO shouldn't pass this, refactor to locations
               :form
               {:action-name "Save"
                :action-method action-method
                :action-target action-target}}]
    {:status 200
     :body (-> {:content (presentation.edit-profile/present model)
                :head-tags head-tags-for-maps}
               layout
               render)}))

(defn- member-input-port
  [req]
  (let [params (get req :params)
        member {:id (get-in req [:session :user-id])
                :name (:member-name params)
                :slug (:member-slug params)
                :avatar (:uploaded-url params)
                :description (:member-description params)}
        location {:latitude (parse-double (:latitude params))
                  :longitude (parse-double (:longitude params))
                  :country-code (:country-code params)
                  :city-id (parse-long (:city-id params))
                  :name (:location-name params)}]
    {:member member :location location}))

(defn create
  [{:keys [create-member-use-case render layout]} req]
  (let [member-params (member-input-port req)
        model (assoc member-params :form (get-in req [:params :form]))
        [status result] (create-member-use-case member-params)]
    (match [status result]
           [:ok _] {:status 303
                    :headers {"Location" (link-for-route req :members/me)}
                    :session (assoc (:session req) :member-id (:id result))}
           [:error _] (-> model presentation.edit-profile/present layout render))))

(defn update-my-profile
  [{:keys [update-member-use-case render layout]} req]
  (let [member-params (member-input-port req)
        model (assoc member-params :form (get-in req [:params :form]))
        [status result] (update-member-use-case member-params)]
    (match [status result]
           [:ok _] {:status 303
                    :headers {"Location" (link-for-route req :members/me)}
                    :session (assoc (:session req) :member-id (:id result))}
           [:error _] (-> model presentation.edit-profile/present layout render))))

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

; TODO See if it's really necessary - by viewing a user who has made a payment,
;      they could be redirected to /members/me/edit page for editing which after saving
;      would create a member for them
(defn new-member
  [{:keys [membership-payment-use-case render layout]} req]
  (let [invoice ()
        model {}]
    {:status 200 :body (-> model presentation.new-member/present layout render)}))

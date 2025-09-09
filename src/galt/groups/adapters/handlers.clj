(ns galt.groups.adapters.handlers
  (:require
    [galt.core.adapters.link-generator :refer [link-for-route]]
    [starfederation.datastar.clojure.api :as d*]
    [galt.groups.adapters.views :as views]
    [galt.groups.adapters.view-models :as models]
    [galt.groups.domain.group-repository :as gr]
    [galt.core.infrastructure.web.helpers :refer [get-signals]]
    [galt.locations.domain.location-repository :as lr]
    [galt.groups.domain.use-cases.add-group :refer [add-group-use-case]]
    [galt.core.views.datastar-helpers :refer [d*-backend-action]]
    [galt.core.adapters.sse-helpers :refer [with-sse]]))

(defn scittle-tag
  [file-name]
  [:script {:type "application/x-scittle" :src (str "/assets/scittle/" file-name)}])

(def head-tags-for-maps
  [[:link {:rel "stylesheet"
           :href "https://unpkg.com/leaflet@2.0.0-alpha.1/dist/leaflet.css"}]
   [:script {:src "https://unpkg.com/leaflet@2.0.0-alpha.1/dist/leaflet-global.js"}]
   (scittle-tag "geocoding-map.cljs")])

(defn new-group
  [{:keys [render layout location-repo]} req]
  (let [countries (lr/all-countries location-repo)
        model {:form {:action-name "Create"
                      :action-method :post
                      :action-target (link-for-route req :groups)}
               :countries countries
               :group {}}]
    {:status 200 :body (render (layout {:content (views/new-group model)
                                        :head-tags head-tags-for-maps}))}))

(defn- ->int [s]
  (try
    (Integer/parseInt s)
    (catch NumberFormatException _e
      nil)))

; TODO add spec for deps (to have repo key with correct type)
(defn create-group
  [{:keys [render layout content] :as deps} req]
  (let [params (get req :params)
        group-creation {:founder-id (get-in req [:session :user-id])
                        :name (:group-name params)
                        :avatar (:uploaded-url params)
                        :description (:group-description params)
                        :location
                        {:latitude (parse-double (:latitude params))
                         :longitude (parse-double (:longitude params))
                         :name (:location-name params)
                         :country-code (:country-code params)
                         :city-id (->int (:city-id params))}}
        [status group errors] (add-group-use-case deps group-creation)]
    (case status
      :ok {:status 303 :headers {"Location" (link-for-route req :groups/by-id {:id (:id group)})}}
      :error {:status 400
              :body (-> [:div
                         (views/error-messages errors)
                         (views/new-group (assoc group-creation :action-name "Create"))]
                        content
                        layout
                        render)})))

(defn edit-group
  [{:keys [edit-group-use-case location-repo content render layout]} req]
  (let [group-id (parse-uuid (get-in req [:path-params :id]))
        logged-in-user-id (get-in req [:session :user-id])
        [status result] (edit-group-use-case {:group-id group-id :editor-id logged-in-user-id})
        location (lr/find-location-by-id location-repo (:location-id result))
        delete-url (link-for-route req :groups/by-id {:id group-id})
        model {:group result
               :location location
               :countries (lr/all-countries location-repo)
               :form
               {:action-name "Save"
                :action-method "PUT"
                :action-target (link-for-route req :groups/by-id {:id group-id})
                :delete-action (d*-backend-action delete-url :delete)}}]
    (if (= status :ok)
      {:status 200 :body (render (layout {:content (views/edit-group model)
                                          :head-tags head-tags-for-maps}))}
      {:status 401 :body (render (layout (content (views/error-messages [(:message result)]))))})))

(defn update-group
  [{:keys [update-group-use-case]} req]
  (let [group-id (get-in req [:path-params :id])
        command {:group {:id (parse-uuid group-id)} :location {}}
        [status result] (update-group-use-case command)]
    (if (= :ok status)
      {:status 303 :headers {"Location" (link-for-route req :groups/by-id {:id (:id result)})}}
      {:status 401 :body result})))

(defn delete-group
  [{:keys [delete-group-use-case]} req]
  (let [group-id (parse-uuid (get-in req [:path-params :id]))
        logged-in-user-id (get-in req [:session :user-id])]
    (delete-group-use-case {:deletor-id logged-in-user-id :group-id group-id})
    (with-sse req (fn [send!]
                    (send! :notification "Group successfully deleted")
                    (send! :js "window.location.href = 'https://dev.galt.is/groups'")))))

(defn list-groups
  [{:keys [render layout] :as deps} req]
  (let [view-model (models/groups-view-model deps req)
        content (views/groups-list view-model)]
    (if (d*/datastar-request? req)
      (with-sse req (fn [send!]
                      (send! :html (layout content) "/groups")))
      {:status 200 :body (render (layout content))})))

(defn show-group
  [{:keys [layout render] :as deps} req]
  (let [group-id (get-in req [:path-params :id])
        model (models/group-model deps req (parse-uuid group-id))
        ]
    {:status 200 :body (render (layout {:content (views/show-group model)
                                        :head-tags head-tags-for-maps}))}))
(defn dropdown-item
  [{:keys [name value extra]}]
  [:div.dropdown-item {:data-on-click
                       (str "$search = '" name "'; $show-results = false;"
                            (d*-backend-action "/groups/search"
                                               :get
                                               {:action "choose" :id value}
                                               {:filter-signals {:include "/action|search|show-results/"}}))}
   name
   (when extra [:span.is-pulled-right {:style {:margin-left "1em"}} extra])
   ])

(defn search-groups
  [{:keys [group-repo]} req]
  (let [signals (get-signals req)
        action (get-in req [:params :action])
        query (:group-name signals)
        fuzzy-find-groups (fn [q] (->> (gr/fuzzy-find-group group-repo q)
                                       (map (fn [g] {:name (:name g) :value (:id g)}) ,,,)))]
    (with-sse req
      (fn [send!]
        (case action
          "search"
          (do
            (send! :html [:div.dropdown-menu {:id "group-search-dropdown-container"}
                          [:div.dropdown-content (map dropdown-item (fuzzy-find-groups query))]])
            (send! :signals {:show-results true}))
          "choose"
          (send! :signals {:group-id (get-in req [:params :id])
                           :group-name (->> [:params :id]
                                            (get-in req ,,,)
                                            (parse-uuid ,,,)
                                            (gr/find-group-by-id group-repo)
                                            (:groups/name ,,,)
                                            )
                           :show-results false}))))))

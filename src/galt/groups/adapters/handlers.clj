(ns galt.groups.adapters.handlers
  (:require
   [galt.core.adapters.link-generator :refer [link-for-route]]
   [galt.core.adapters.sse-helpers :refer [with-sse]] ; List-groups deps
   [galt.core.adapters.time-helpers :as time-helpers]
   [galt.core.adapters.url-helpers :refer [add-query-params]]
   [galt.core.infrastructure.web.helpers :refer [get-signals]]
   [galt.core.views.datastar-helpers :refer [d*-backend-action]]
   [galt.groups.adapters.presentation.list-groups :as presentation.list-groups]
   [galt.groups.adapters.presentation.show-group :as presentation.show-group]
   [galt.groups.adapters.views :as views]
   [galt.groups.domain.group-repository :as gr]
   [galt.locations.domain.location-repository :as lr]
   [ring.util.http-status :as http-status]
   [starfederation.datastar.clojure.api :as d*]))

(defn scittle-tag
  [file-name]
  [:script {:type "application/x-scittle" :src (str "/assets/scittle/" file-name)}])

(def head-tags-for-maps
  [[:link {:rel "stylesheet"
           :href "https://unpkg.com/leaflet@2.0.0-alpha.1/dist/leaflet.css"}]
   [:script {:src "https://unpkg.com/leaflet@2.0.0-alpha.1/dist/leaflet-global.js"}]
   (scittle-tag "geocoding-map.cljs")])

(defn new-group
  [{:keys [render layout location-repo content new-group-use-case]} req]
  (let [countries (lr/all-countries location-repo) ; TODO move this to locations UI & handler
        model {:form {:action-name "Create"
                      :action-method :post
                      :action-target (link-for-route req :groups)}
               :countries countries
               :group {}}
        [status result] (new-group-use-case {:user-id (get-in req [:session :user-id])})]
    (case status
      :ok {:status 200 :body (render (layout {:content (views/new-group model)
                                              :head-tags head-tags-for-maps}))}
      :error {:status 403 :body (-> (views/error-messages [result]) content layout render)})))

(defn- ->int [s]
  (try
    (Integer/parseInt s)
    (catch NumberFormatException _e
      nil)))

; TODO add spec for deps (to have repo key with correct type)
(defn create-group
  [{:keys [render layout content add-group-use-case] :as deps} req]
  (let [params (get req :params)
        group-creation {:founder-id (get-in req [:session :member-id])
                        :name (:group-name params)
                        :avatar (:uploaded-url params)
                        :description (:group-description params)
                        :location
                        {:latitude (parse-double (:latitude params))
                         :longitude (parse-double (:longitude params))
                         :name (:location-name params)
                         :country-code (:country-code params)
                         :city-id (->int (:city-id params))}}
        [status group errors] (add-group-use-case group-creation)]
    (case status
      :ok {:status 303 :headers {"Location" (link-for-route req :groups/by-id {:id (:id group)})}}
      :error {:status 400
              :body (-> (views/error-messages errors)
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
               :countries (lr/all-countries location-repo) ; TODO shouldn't pass this, refactor to locations
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

(defn add-group-links
  [req group]
  (assoc group :group-link (link-for-route req :groups/by-id {:id (:id group)})))

(defn list-groups
  [{:keys [render list-groups-use-case layout] :as deps} req]
  (if (d*/datastar-request? req)
    (with-sse req
      (fn [send!]
        (let [signals (get-signals req)
              patch-mode (get-in req [:params :patch-mode])
              limit (if (= patch-mode "inner") 5 (get signals :limit 5))
              offset (if (= patch-mode "inner") 0 (get signals :offset 0))
              next-offset (+ limit offset)
              query-str (get signals :query "")
              command {:limit limit :offset offset :query query-str}
              [status result] (list-groups-use-case command)
              model (->> (:groups result)
                         (map (partial add-group-links req) ,,,)
                         (map (fn [g] (assoc g :location (get-in result [:locations (:id g)]))) ,,,))]
          (send! :html (map presentation.list-groups/group-row model) {:selector "#group-rows"
                                                                       :patch-mode patch-mode})
          (send! :signals {:offset next-offset :limit limit}))))
    (let [limit 5
          offset 0
          [status result] (list-groups-use-case {:query "" :limit limit :offset offset})
          groups (->> (:groups result)
                         (map (partial add-group-links req) ,,,)
                         (map (fn [g] (assoc g :location (get-in result [:locations (:id g)]))) ,,,))
          model {:new-group-href (link-for-route req :groups/new)
                 :groups groups
                 :location (:locations result)
                 :initial-signals "{offset: 5, limit: 5}"
                 :offset offset
                 :limit limit
                 }]
      {:status http-status/ok
       :body (-> model presentation.list-groups/present layout render)})))

(defn show-group
  [{:keys [layout render show-group-use-case] :as deps} req]
  (let [group-id (get-in req [:path-params :id])
        viewing-user-id (get-in req [:session :user-id])
        [status result] (show-group-use-case {:viewing-user-id viewing-user-id
                                              :group-id (parse-uuid group-id)})
        {:keys [group location members]} result
        model {:name (:name group)
               :description (:description group)
               :avatar (:avatar group)
               :languages ["Spanish" "English"]
               :location-name (:name location)
               :latitude (:latitude location)
               :longitude (:longitude location)
               :founded-at (time-helpers/relative-with-short (:created-at group))
               :new-post-href (-> (link-for-route req :posts/new)
                                  (add-query-params ,,, {:target-id group-id :target-type "group"}))
               :members (map (fn [m] {:name (:name m)
                                      :href (link-for-route req :members/by-id {:id (:id m)})})
                             members)
               :activity (:posts result)}]
    {:status 200 :body (render (layout {:content (presentation.show-group/present model)
                                        :head-tags head-tags-for-maps}))}))

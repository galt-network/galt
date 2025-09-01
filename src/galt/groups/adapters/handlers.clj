(ns galt.groups.adapters.handlers
  (:require
    [galt.core.infrastructure.web.helpers :refer [get-signals]]
    [galt.core.adapters.link-generator :refer [link-for-route]]
    [starfederation.datastar.clojure.api :as d*]
    [galt.groups.adapters.views :as views]
    [galt.groups.adapters.view-models :as models]
    [galt.groups.domain.group-repository :as gr]
    [galt.groups.domain.use-cases.add-group :refer [new-add-group-use-case]]
    [galt.groups.domain.use-cases.edit-group :refer [edit-group-use-case]]
    [galt.core.adapters.sse-helpers :refer [with-sse]]))

(defn new-group
  [{:keys [render content layout]} req]
  (if (d*/datastar-request? req)
    (with-sse
      req
      (fn [send!]
        (send! :html (content (views/create-group-form nil)) "/groups/new")
        (send! :cljs '(println "Hello from Scittle!"))))
    {:status 200 :body (render (layout (views/create-group-form nil)))}))

; TODO add spec for deps (to have repo key with correct type)
(defn create-group
  [{:keys [group-repo content] :as deps} req]
  (let [create-new-group (new-add-group-use-case {:group-repo group-repo
                                                            :uuid (:uuid deps)})
        signals (get-signals req)
        group-creation {:founder-id (get-in req [:session :user-id])
                        :name (:group-name signals)
                        :avatar (:uploaded-url signals)
                        :description (:group-description signals)}
        [status group errors] (create-new-group group-creation)
        model (case status
                :ok {:success? true
                     :content (content (views/show-group (models/group-model deps req (:groups/id group))))
                     :message (str "You successfully created a group "
                                   (:groups/name group)
                                   " and were added as owner.")}
                :error {:success? false
                        :content (content [:div
                                           [:article.message.is-danger
                                            [:div.message-header
                                             [:p "Errors in creating group"]]
                                            [:div.message-body
                                             [:ul (map (fn [e] [:li e]) errors)]]]
                                           (views/create-group-form nil)])
                        :message "Unable to create group"})]
    (with-sse
      req
      (fn [send!]
        (send! :html (:content model))
        (send! :notification (:message model) :is-danger)))))

(defn error-message [message]
  [:article.message.is-danger
   [:div.message-header
    [:p "Unauthorized Error"]]
   [:div.message-body message]])

(defn edit-group
  [{:keys [group-repo content render layout]} req]
  (let [group-id (parse-uuid (get-in req [:path-params :id]))
        logged-in-user-id (get-in req [:session :user-id])
        [status result] (edit-group-use-case {:group-repo group-repo}
                                                   {:group-id group-id :editor-id logged-in-user-id})]
    (if (d*/datastar-request? req)
      (with-sse
        req
        (fn [send!]
          (if (= status :ok)
            (send! :html
                 (content (views/group-form result))
                 (link-for-route req :groups/edit-group {:id group-id}))
            (send! :notification (:message result) :is-danger))))
      (if (= status :ok)
        {:status 200 :body (render (layout (views/group-form result)))}
        {:status 401 :body (render (layout (content (error-message (:message result)))))}))))

(defn update-group
  [{:keys [content group-repo]} req]
  (let [group-id (get-in req [:query-params :id])
        updated-group (gr/update-group group-repo group-id "Hardcoded Name" "Hardcoded Description")]
    (with-sse
      req
      (fn [send!]
        (send! :html (content (views/show-group updated-group)))))))

(defn list-groups
  [{:keys [render layout] :as deps} req]
  (let [view-model (models/groups-view-model deps req)
        content (views/groups-list view-model)]
    (if (d*/datastar-request? req)
      (with-sse req (fn [send!] (send! :html (layout content) "/groups")))
      {:status 200 :body (render (layout content))})))

(defn show-group
  [{:keys [layout render content] :as deps} req]
  (let [group-id (get-in req [:path-params :id])
        model (models/group-model deps req (parse-uuid group-id))]
    (if (d*/datastar-request? req)
        (with-sse
          req
          (fn [send!]
            (send! :html (content (views/show-group model)) (str "/groups/" group-id))))
        {:status 200 :body (render (layout (views/show-group model)))})))

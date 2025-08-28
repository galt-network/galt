(ns galt.groups.adapters.handlers
  (:require
    [galt.core.infrastructure.web.helpers :refer [get-signals]]
    [starfederation.datastar.clojure.api :as d*]
    [galt.groups.adapters.views :as views]
    [galt.groups.adapters.view-models :as models]
    [galt.groups.domain.group-repository :as gr]
    [galt.groups.domain.use-cases.add-group :as use-cases]
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
  [{:keys [group-repo content]} req]
  (let [use-case (use-cases/new-add-group-use-case group-repo)
        signals (get-signals req)
        creator-id (get-in req [:session :user-id])
        [status result] (use-case creator-id (:group-name signals) (:group-description signals))
        model (case status
                :ok {:success? true
                     :message (str "You successfully created a group "
                                   (:groups/name result)
                                   " and were added as owner.")}
                :error {:success? false
                        :message "Unable to create group"})]
    (with-sse
      req
      (fn [send!]
        (send! :html (content (views/show-group result)))
        (send! :notification (:message model))))))

; TODO: implement
(defn edit-group
  [{:keys [group-repo]} req]
  (let [group-id (get-in req [:params :group-id])
        group (gr/find-group-by-id group-repo group-id)]
    {:status 200 :body (str group)}))

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
        model (models/group-model deps req group-id)]
    (if (d*/datastar-request? req)
        (with-sse
          req
          (fn [send!]
            (send! :html (content (views/show-group model)) (str "/groups/" group-id))))
        {:status 200 :body (render (layout (views/show-group model)))})))

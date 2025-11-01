(ns galt.comments.adapters.comment-handlers
  (:require
   [galt.comments.adapters.presentation :as presentation]
   [galt.comments.domain.comment-repository :as cr]
   [galt.comments.domain.entities.comment :refer [nest-comments]]
   [galt.core.adapters.link-generator :refer [link-for-route]]
   [galt.core.adapters.number-helpers :refer [->int]]
   [galt.core.adapters.sse-helpers :refer [with-sse]]
   [galt.core.adapters.time-helpers :as th]
   [galt.core.views.datastar-helpers :refer [d*-backend-action]]
   [ring.util.http-status :as http-status]))

(defn send-comment
  [{:keys [comment-repo]} req]
  (let [entity-type (keyword (get-in req [:path-params :entity-type]))
        entity-id (parse-uuid (get-in req [:path-params :entity-id]))
        parent-id (some-> (get-in req [:params :parent-id]) parse-long)
        content (get-in req [:params :content])
        author-id (get-in req [:session :member-id])
        return-to-url (get-in req [:params :return-to])
        comment {:parent-id parent-id :content content :author-id author-id}]
    (cr/add-comment comment-repo entity-id entity-type comment)
    {:status http-status/see-other :headers {"Location" return-to-url}}))

(defn add-datastar-action
  [base-route comment]
  (assoc comment
         :datastar-modal-action
         (d*-backend-action base-route :get {:action "open-modal" :parent-id (:id comment)})))

(defn show-comments
  [{:keys [comment-repo]} req]
  (let [entity-type (keyword (get-in req [:path-params :entity-type]))
        entity-id (parse-uuid (get-in req [:path-params :entity-id]))
        action (keyword (get-in req [:params :action]))
        route-name (case entity-type :events :events/by-id :posts :posts/by-id)
        entity-url (link-for-route req route-name {:id entity-id})
        parent-id (->int (get-in req [:params :parent-id]))
        query-params (cond-> {}
                             (= action :open-modal) (assoc ,,, :comment-id parent-id))
        comments (cr/list-comments comment-repo entity-id entity-type query-params)
        comment-base-url (link-for-route req :comments {:entity-id entity-id :entity-type entity-type})
        rich-comments (->> comments
                           (map (fn [c] (assoc c :created-at (th/short-format-with-time (:created-at c)))) ,,,)
                           (map (partial add-datastar-action comment-base-url) ,,,)
                           nest-comments)
        add-comment-action (link-for-route req :comments {:entity-id entity-id :entity-type entity-type})
        model {:add-comment-action add-comment-action
               :return-to entity-url
               :parent-id nil
               :comments rich-comments}]
    (with-sse req
      (fn [send!]
        (case action
          :open-modal
          (do
            (send! :html
                   (presentation/comment-form-modal (merge (first rich-comments)
                                                           {:parent-id parent-id
                                                            :add-comment-action add-comment-action
                                                            :return-to entity-url}))
                   {:selector "#comment-modal" :patch-mode "inner"})
            (send! :signals {:show-comment-modal true}))
          ; Default action
          (send! :html (presentation/present model) {:selector "#comments"}))))))

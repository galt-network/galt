(ns galt.posts.adapters.handlers
  (:require
   [clojure.core.match :as m]
   [galt.core.adapters.link-generator :refer [link-for-route]]
   [galt.core.adapters.sse-helpers :refer [with-sse]]
   [galt.core.adapters.time-helpers :as th]
   [galt.core.infrastructure.web.helpers :refer [get-signals]]
   [galt.core.views.datastar-helpers :refer [d*-backend-action]]
   [galt.posts.adapters.presentation.list-posts :as presentation.list-posts]
   [galt.posts.adapters.presentation.new-post :as presentation.new-post]
   [galt.posts.adapters.presentation.show-post :as presentation.show-post]
   [galt.posts.domain.post-repository :as pr]
   [ring.util.http-status :as http-status]
   [starfederation.datastar.clojure.api :refer [datastar-request?]]))

(defn- add-post-links
  [req post]
  (assoc post :post-link (link-for-route req :posts/by-id {:id (:id post)})))

(defn- list-posts-use-case
  [{:keys [post-repo]}
   {:keys [limit offset from-date to-date]}]
  (let [posts (pr/list-posts post-repo {:limit limit
                                        :offset offset
                                        :from-date from-date
                                        :to-date to-date})]
    [:ok posts]))

(defn list-posts
  [{:keys [render layout] :as deps} req]
  (if (datastar-request? req)
    (with-sse req
      (fn [send!]
        (let [signals (get-signals req)
              patch-mode (get-in req [:params :patch-mode])
              limit 10
              offset (if (= patch-mode "inner") 0 (get signals :offset 0))
              next-offset (+ limit offset)
              period (keyword (get signals :period "this-month"))
              [start-time end-time] (th/period-range period)
              command {:limit limit :offset offset :from-date start-time :to-date end-time}
              [status result] (list-posts-use-case deps command)
              model (map (partial add-post-links req) result)]
          (send! :html (map presentation.list-posts/post-card model) {:selector "#post-cards"
                                                                         :patch-mode patch-mode})
          (send! :signals {:offset next-offset :limit limit}))))
    (let [limit 5
          offset 0
          [start-time end-time] (th/period-range :this-month)
          [status result] (list-posts-use-case deps {:limit limit
                                                     :offset offset
                                                     :start-time start-time
                                                     :end-time end-time})
          model {:new-post-href (link-for-route req :posts/new)
                 :posts (map (partial add-post-links req) result)
                 :initial-signals "{offset: 5, limit: 5}"
                 :offset offset
                 :limit limit}]
      {:status http-status/ok
       :body (-> model presentation.list-posts/present layout render)})))

(defn new-post
  [{:keys [render layout]} req]
  (let [model {:form-action (link-for-route req :posts)
               :form-method "POST"
               :post {:target-id (get-in req [:query-params "target-id"])
                     :target-type (get-in req [:query-params "target-type"])}}]
    {:status 200 :body (-> model presentation.new-post/present layout render)}))

(defn create-post
  [{:keys [render layout create-post-use-case]} req]
  (let [author-id (get-in req [:session :member-id])
        params (get req :params)
        command {:post {:author-id author-id
                        :title (:title params)
                        :content (:content params)
                        :target-type (:target-type params)
                        :target-id (parse-uuid (:target-id params))
                        :comments-policy "everybody"}}
        ; TODO Add validations to use case, show validation errors in the form
        [status result] (create-post-use-case command)
        model {}]
    (case status
      :ok {:status 302 :headers {"Location" "/posts"}}
      :error {:status 422 :body (-> model presentation.new-post/present layout render)})))

(defn show-post
  [{:keys [render post-repo layout]} req]
  (let [member-id (get-in req [:session :member-id])
        post-id (parse-uuid (get-in req [:path-params :id]))
        post (pr/get-post post-repo post-id)
        author-viewing? (= member-id (:author-id post))
        comments-url (link-for-route req :comments {:entity-id post-id :entity-type "posts"})
        edit-href (link-for-route req :posts.by-id/edit {:id (:id post)})
        model {:post post
               :edit-href (when author-viewing? edit-href)
               :comment-action (d*-backend-action comments-url :get)}]
    {:status http-status/ok
     :body (-> model presentation.show-post/present layout render)}))


(defn edit-post
  [{:keys [render post-repo layout]} req]
  (let [post-id (parse-uuid (get-in req [:path-params :id]))
        post (pr/get-post post-repo post-id)
        model {:post post
               :form-action (link-for-route req :posts.by-id/edit {:id post-id})
               :form-method "PUT"}]
    {:status http-status/ok
     :body (-> model presentation.new-post/present layout render)}))

(defn update-post
  [{:keys [render update-post-use-case layout]} req]
  (let [post-id (parse-uuid (get-in req [:path-params :id]))
        member-id (get-in req [:session :member-id])
        post {:title (get-in req [:params :title])
              :content (get-in req [:params :content])}
        command {:post-id post-id
                 :updater-id member-id
                 :post post}
        [status result] (update-post-use-case command)
        model {:post post
               :form-action (link-for-route req :posts.by-id/edit {:id post-id})
               :form-method "PUT"}]
    (m/match [status result]
           [:ok _] {:status http-status/see-other
                    :headers {"Location" (link-for-route req :posts/by-id {:id post-id})}}
           [:error _] {:status http-status/unprocessable-entity
                       :body (-> (assoc model :errors result) presentation.new-post/present layout render)})))

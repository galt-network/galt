(ns galt.posts.adapters.handlers
  (:require
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
  (let [model {:target-id (get-in req [:query-params "target-id"])
               :target-type (get-in req [:query-params "target-type"])}]
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
  (let [post-id (parse-uuid (get-in req [:path-params :id]))
        post (pr/get-post post-repo post-id)
        comments-url (link-for-route req :comments {:entity-id post-id :entity-type "posts"})
        model {:post post
               :comment-action (d*-backend-action comments-url :get)}]
    {:status http-status/ok
     :body (-> model presentation.show-post/present layout render)}))

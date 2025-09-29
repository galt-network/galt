(ns galt.posts.adapters.handlers
  (:require
    [galt.posts.adapters.presentation.new-post :as presentation.new-post]
    [galt.posts.adapters.presentation.show-posts :as presentation.show-posts]
    [galt.posts.domain.post-repository :as pr]))

(defn list-posts
  [{:keys [render post-repo layout]} req]
  (let [user-groups [(parse-uuid "01997bba-dbe1-7067-9135-1a300d70e218")]
        posts (pr/group-posts post-repo user-groups)
        model {:posts posts}]
    {:status 200 :body (-> model presentation.show-posts/present layout render)}))

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

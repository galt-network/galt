(ns galt.posts.external.routes
  (:require
    [reitit.ring :as rr]
    [galt.posts.adapters.handlers :as handlers]))

(defn router
  [deps]
  (let [with-layout (:with-layout deps)
        with-deps-layout (partial with-layout deps)]
    (rr/router
      [["/posts" {:id :posts
                  :name :posts
                  :post (with-deps-layout handlers/create-post)
                  :get (with-deps-layout handlers/list-posts)}]
       ["/posts/new" {:id :posts
                      :name :posts/new
                      :conflicting true
                      :get (with-deps-layout handlers/new-post)}]
       ["/posts/:id"
        ["" {:id :posts
             :conflicting true
             :name :posts/by-id
             :get (with-deps-layout handlers/show-post)}]
        ["/edit" {:id :posts
                  :name :posts.by-id/edit
                  :get (with-deps-layout handlers/edit-post)
                  :put (with-deps-layout handlers/update-post)}]]])))

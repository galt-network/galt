(ns galt.posts.adapters.handlers-test
  (:require
    [clojure.string :as str]
    [clojure.test :refer [deftest is]]
    [ring.mock.request :as mock]
    [java-time.api :as jt]
    [galt.posts.adapters.handlers :as handlers]))

(defn- fake-post-repo [posts]
  (reify galt.posts.domain.post-repository/PostRepository
    (list-posts [_ _opts] posts)))

(defn- stub-deps [& {:as extra}]
  (merge {          :link-for-route (fn [route-name & [_path-params]]
                            (str "/" (subs (str route-name) 1)))
          :post-repo (fake-post-repo [{:id 1 :title "Test post" :content "Content"
                                       :author-avatar nil :author "Tester"
                                       :author-id 42
                                       :publish-at (jt/local-date-time 2026 1 1 12 0)}])
          :render identity
          :layout (fn [model] model)
          :asset-url identity}
         extra))

(deftest list-posts-renders-post-links
  (is (= 200
         (:status (handlers/list-posts (stub-deps) (mock/request :get "/posts"))))))

(deftest list-posts-uses-deps-link-for-route
  (let [resp (handlers/list-posts (stub-deps) (mock/request :get "/posts"))]
    (is (str/includes? (str (:body resp)) "/posts/by-id"))))

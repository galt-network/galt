(ns galt.posts.adapters.presentation.show-post
  (:require
   [galt.core.adapters.presentation-helpers :refer [render-markdown]]))

(defn present
  [{:keys [post comment-action]}]
  [:div
   [:h1.title.is-2 (:title post)]
   [:div {:class "content"} (render-markdown (:content post))]
   [:div#comments {:data-init comment-action}]])

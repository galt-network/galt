(ns galt.posts.adapters.presentation.show-posts
  (:require
    [galt.groups.adapters.presentation.show-group :refer [activity-card]]))

(defn present
  [model]

  [:div
   [:h1.title.is-2 "Posts"]
   (map activity-card (:posts model))])

(ns galt.events.adapters.presentation.show-event
  (:require
    [galt.core.adapters.presentation-helpers :refer [render-markdown]]))

(defn present
  [{:keys [event comment-action]}]
  [:div
   [:h1.title.is-2 (:name event)]
   [:div {:class "content"} (render-markdown (:description event))]
   [:div#comments {:data-init comment-action}]])

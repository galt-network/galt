(ns galt.posts.adapters.presentation.show-post)


(defn present
  [{:keys [event comments add-comment-action]}]
  (let []
    [:div
     [:h1.title.is-2 (:name event)]
     [:div {:class "content"}
      (render-markdown (:description event))]
     (add-comment-form {:add-comment-action add-comment-action :parent-id nil})
     (for [comment-model comments] (comment (assoc comment-model :add-comment-action add-comment-action)))
     [:div.modal {:data-class-is-active "$show-comment-modal"}
      [:div.modal-background]
      [:div.modal-content {:id "comment-modal"}]
      [:button.modal-close.is-large {:data-on-click "$show-comment-modal = false"}]]]))

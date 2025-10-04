(ns galt.events.adapters.presentation.show-event
  (:require
    [galt.core.adapters.presentation-helpers :refer [render-markdown]]
    [galt.core.views.datastar-helpers :refer [d*-backend-action]]))

(defn add-comment-form
  [{:keys [parent-id add-comment-action]}]
  [:form {:method "POST" :action add-comment-action}
     [:input {:type "hidden" :name "parent-id" :value parent-id}]
     [:div.field
      [:textarea.textarea {:name "content" :rows 3}]]
     [:div.field
      [:button.button "Send"]]])

(defn comment-body
  [model & [extra]]
  [:article.media
   [:figure.media-left
    [:p.image.is-64x64
     [:img {:src (:author-avatar model)}]]]
   [:div.media-content
    [:div
     [:strong (:author-name model)]
     [:br]
     (:content model)
     [:br]
     [:small (:created-at model)]
     extra]]])

(defn comment
  [model]
  (comment-body
    model
    (list
      (when (> 2 (:level model))
        [:small
         " • "
         [:a {:data-on-click (:datastar-modal-action model)} "Reply"]])
      (for [comment-model (:replies model)] (comment comment-model)))))

#_ (defn comment
  [model]
  [:article.media
   [:figure.media-left
    [:p.image.is-64x64
     [:img {:src (:author-avatar model)}]]]
   [:div.media-content
    [:div
     [:p
      [:strong (:author-name model)]
      [:br]
      (:content model)
      [:br]
      [:small (:created-at model)]
      (when (> 2 (:level model))
        [:small
         " • "
         [:a {:data-on-click (:datastar-modal-action model)} "Reply"]])]]
    (for [comment-model (:replies model)] (comment comment-model))]])

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

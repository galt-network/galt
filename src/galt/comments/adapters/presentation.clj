(ns galt.comments.adapters.presentation)

(defn add-comment-form
  [{:keys [parent-id add-comment-action return-to]}]
  [:form {:method "POST" :action add-comment-action}
   [:input {:type "hidden" :name "parent-id" :value parent-id}]
   [:input {:type "hidden" :name "return-to" :value return-to}]
   [:div.field [:textarea.textarea {:name "content" :rows 3}]]
   [:div.field [:button.button "Send"]]])

(defn comment-body
  [{:keys [author-avatar author-name content created-at]} & [extra]]
  [:article.media
   [:figure.media-left
    [:p.image.is-64x64
     [:img {:src author-avatar}]]]
   [:div.media-content
    [:div
     [:strong author-name]
     [:br]
     content
     [:br]
     [:small created-at]
     extra]]])

(defn show-comment
  [model]
  (comment-body
    model
    (list
      (when (> 2 (:level model))
        [:small
         " • "
         [:a {:data-on:click (:datastar-modal-action model)} "Reply"]])
      (for [comment-model (:replies model)] (show-comment comment-model)))))

(defn comment-form-modal
  [model]
  [:div.card
   [:div.card-content
    (comment-body model)
    (add-comment-form model)]])

(defn present
  [{:keys [add-comment-action comments return-to]}]
  [:div#comments
   (add-comment-form {:add-comment-action add-comment-action
                      :parent-id nil
                      :return-to return-to})
   (for [comment-model comments] (show-comment (assoc comment-model :add-comment-action add-comment-action)))
   [:div.modal {:data-class:is-active "$show-comment-modal"}
    [:div.modal-background]
    [:div.modal-content {:id "comment-modal"}]
    [:button.modal-close.is-large {:data-on:click "$show-comment-modal = false"}]]])

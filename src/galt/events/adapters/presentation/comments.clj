(ns galt.events.adapters.presentation.comments)

(defn comment-form
  [model]
  [:div {:id (:commented-event model)}
   [:div.field
    [:textarea.textarea]]
   [:div.field.is-grouped
    [:button.button
     {:data-on-click (:clear-action model)}
     "Close"]
    [:button.button.is-primary
     {:data-on-click (:send-action model)}
     "Send"]]])

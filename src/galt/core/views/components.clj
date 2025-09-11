(ns galt.core.views.components)

(defn errors-list
  ([] (errors-list []))
  ([errors]
   (let [errors (if (string? errors) [errors] errors)]
      [:div#error-messages-container
       (when (seq errors)
         [:article.message.is-danger
          [:div.message-header [:p "Error in showing profile"]]
          [:div.message-body [:ul (map (fn [e] [:li e]) errors)]]]) ])))

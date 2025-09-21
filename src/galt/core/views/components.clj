(ns galt.core.views.components)

(defn errors-list
  ([] (errors-list []))
  ([errors] (errors-list "Something went wrong" errors))
  ([title errors]
   (let [errors (if (string? errors) [errors] errors)]
      [:div#error-messages-container
       (when (seq errors)
         [:article.message.is-danger
          [:div.message-header [:p title]]
          [:div.message-body [:ul (map (fn [e] [:li e]) errors)]]]) ])))

(def message-type->class
  {:success "is-success"
   :info "is-info"
   :warning "is-warning"
   :error "is-danger"})

(defn message
  [{:keys [title content type]}]
  [:article.message {:class [(message-type->class type)]}
   [:div.message-header [:p title]]
   [:div.message-body content]])

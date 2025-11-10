(ns galt.posts.adapters.presentation.new-post
  (:require
    [galt.core.views.components :refer [errors-list]]))

(defn present
  [{:keys [post] :as model}]
  [:div
   (when (:errors model) (errors-list "Error in updating the post" (:errors model)))
   [:form {:method "POST" :action (:form-action model)}
    [:input {:type "hidden" :name "_method" :value (:form-method model)}]
    [:input {:type "hidden" :name "target-type" :value (:target-type post)}]
    [:input {:type "hidden" :name "target-id" :value (:target-id post)}]
    [:div.field
     [:label.label "Title"]
     [:div.control
      [:input.input {:name "title" :value (:title post)}]]]
    [:div.field
     [:label.label "Content"]
     [:div.control
      [:textarea.textarea {:name "content" :rows 20} (:content post)]]
     [:p.help "Can use Markdown"]]
    [:div.field
     [:div.control
      [:button.button.is-primary "Save"]]]]])

(ns galt.posts.adapters.presentation.new-post
  (:require
    [galt.core.views.components :refer [errors-list]]))

(defn present
  [model]
  [:div
   [:form {:method "POST" :action "/posts"}
    [:input {:type "hidden" :name "target-type" :value (:target-type model)}]
    [:input {:type "hidden" :name "target-id" :value (:target-id model)}]
    [:div.field
     [:label.label "Title"]
     [:div.control
      [:input.input {:name "title" :value (:title model)}]]]
    [:div.field
     [:label.label "Content"]
     [:div.control
      [:textarea.textarea {:name "content"} (:content model)]]
     [:p.help "Can use Markdown"]]
    [:div.field
     [:div.control
      [:button.button.is-primary "Save"]]]]])

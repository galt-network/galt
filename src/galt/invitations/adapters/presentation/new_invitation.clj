(ns galt.invitations.adapters.presentation.new-invitation
  (:require
    [galt.core.views.components.dropdown-search :refer [dropdown-search]]))

(defn present
  [{:keys [form expiration-min expiration-max form-action]}]
  [:div
   [:h1.title.is-2 "New invitation"]
   [:form {:action form-action :method "POST"}
    [:div.field
     [:label.label "Group to invite to"]
     (dropdown-search {:name "group-name"
                       :value (:group-name form)
                       :id (:group-name-id form)
                       :endpoint "/groups/search"})]
    [:div.field
     [:label.label "Valid until"]
     [:div.control
      [:input.input {:type "date" :name "expires-at" :min expiration-min :max expiration-max}]]]
    [:div.field
     [:label.label "Max usages"]
     [:div.control
      [:input.input {:type "number" :min 1 :max 100 :step 1 :name "max-usages"}]]
     [:p "Useful if you want to share the same invitation with more people"]]
    [:div.field
     [:label.label "Content"]
     [:div.control
      [:textarea.textarea {:name "content"}]]
     [:p "The users of the invitation will see this text when they open the link"]]
    [:div.field
     [:div.control
      [:button.button.is-primary "Create"]]]]])

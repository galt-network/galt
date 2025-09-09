(ns galt.invitations.adapters.presentation
  (:require
    [galt.core.views.datastar-helpers :refer [d*-backend-action]]))

(defn present
  [model]
  [:div
   [:h1.title.is-2 "Request an invitation"]
   [:div.content
    [:blockquote
     [:p [:i "It is more likely your request will be attended if you pick a group or member to send it to."]]
     [:p [:i "Picking a group allows you to search only within its members. If you leave the member field empty,
             all the members of the picked group with invitation creation rights can answer you, though
             it will most likely be the founder"]]]]
   [:form.form
    [:input {:type "hidden" :data-bind "group-id"}]
    [:div.field
     [:label.label "Group to post the invitation to (optional)"]
     [:div.control
      [:div.dropdown {:data-class-is-active "$show-results"}
       [:div.dropdown-trigger
        [:input.input {:name "group"
                       :data-bind "group-name"
                       :data-on-focus "$show-results = true"
                       :data-on-click__outside "$show-results = false"
                       :data-on-input__debounce.500ms
                       (d*-backend-action "/groups/search"
                                          :get
                                          {:action "search"}
                                          {:filter-signals {:include "/group-name|action|show-results/"}})}]]
       [:div.dropdown-menu {:id "group-search-dropdown-container"}]]]]
    [:div.field
     [:label.label "Member to request the invitation from (optional)"]
     [:div.control
      [:input.input {:name "member"}]]]
    [:div.field
     [:label.label "Your invitation request content"]
     [:div.control
      [:textarea.textarea {:name "content"}]]
     [:p "Describe briefly why you want to become a member of Galt or their group"]]
    [:div.field
     [:div.control
      [:button.button.is-primary "Send"]]]]])

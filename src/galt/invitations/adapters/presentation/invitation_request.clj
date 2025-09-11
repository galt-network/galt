(ns galt.invitations.adapters.presentation.invitation-request
  (:require
   [galt.core.views.components :refer [errors-list]]
   [galt.core.views.components.dropdown-search :refer [dropdown-search]]))


(defn present
  [{:keys [form errors]}]
  [:div
   [:h1.title.is-2 "Request an invitation"]
   [:div.content
    [:blockquote
     [:p [:i "It is more likely your request will be attended if you pick a group or member to send it to."]]
     [:p [:i "Picking a group allows you to search only within its members. If you leave the member field empty,
             all the members of the picked group with invitation creation rights can answer you, though
             it will most likely be the founder"]]]]
   (errors-list errors)
   [:form.form {:action "/invitations/new-request" :method "POST"}
    [:div.field
     [:label.label "Group to post the invitation to (optional)"]
     [:div.control
      (dropdown-search {:name "group-name"
                        :value (:group-name form)
                        :id (:group-name-id form)
                        :extra-signals ["member-name-id"]
                        :endpoint "/groups/search"})]
     [:p {:data-text "$member-name-id && $member-name ? 'Searching only groups of ' + $member-name : ''"}]]
    [:div.field
     [:label.label "Member to request the invitation from (optional)"]
     [:div.control
      (dropdown-search {:name "member-name"
                        :value (:member-name form)
                        :id (:member-name-id form)
                        :extra-signal "group-name-id"
                        :endpoint "/members/search"})]
     [:p {:data-text "$group-name-id && $group-name ? 'Searching only members of ' + $group-name : ''"}]]
    [:div.field
     [:label.label "Email to contact you at"]
     [:div.control
      [:input.input {:name "email" :type "email" :value (:email form)}]]]
    [:div.field
     [:label.label "Your invitation request content"]
     [:div.control
      [:textarea.textarea {:name "content" :minlength 30} (:content form)]]
     [:p "Describe briefly why you want to become a member of Galt or their group"]]
    [:div.field
     [:div.control
      [:button.button.is-primary "Send"]]]]])

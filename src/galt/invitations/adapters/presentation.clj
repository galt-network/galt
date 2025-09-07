(ns galt.invitations.adapters.presentation)

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
    [:div.field
     [:label.label "Group to post the invitation to (optional)"]
     [:div.control
      [:input.input {:name "group"}]]]
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

(ns galt.invitations.adapters.presentation.invitations-dashboard)

(defn new-invitation-section
  []
  [:section.section
   [:div.content
    [:blockquote
     [:p
      [:i "Users need to become members in order to use all features of Galt.
          They can do it through an invitation"]]
     [:p
      [:i "After you create an invitation, you can share it as a link or QR code.
          Invitations can be configured to be used once or multiple times and to have expiration time"]]]]
   [:a.button.is-primary.is-medium {:href "/invitations/new"} "Create new invitation"]])

(defn present
  [{:keys [requests invitations]}]
  [:div {:class "container"}
   (new-invitation-section)
   [:section {:class "section"}
    [:h1 {:class "title"} "Invitation Requests"]
    [:table {:class "table is-striped is-hoverable is-fullwidth"}
     [:thead
      [:tr
       [:th "Requesting User"]
       [:th "To Member"]
       [:th "To Group"]
       [:th "Actions"]]]
     [:tbody
      (for [req requests]
        [:tr
         [:td (:requesting-user req)]
         [:td (:to-member req)]
         [:td (:to-group req)]
         [:td
          [:button {:class "button is-success"} "Approve"]
          [:button {:class "button is-danger ml-2"} "Deny"]]])]]]
   [:section {:class "section"}
    [:h1 {:class "title"} "Past Invitations"]
    [:table {:class "table is-striped is-hoverable is-fullwidth"}
     [:thead
      [:tr
       [:th "Inviting Member"]
       [:th "Invited User"]
       [:th "Created At"]
       [:th "Expires At"]
       [:th "Max Usages"]
       [:th "Current Usages"]
       [:th "Actions"]]]
     [:tbody
      (for [inv invitations]
        [:tr
         [:td (:inviter inv)]
         [:td (:invited-user inv)]
         [:td (:created-at inv)]
         [:td (:expires-at inv)]
         [:td (:max-usages inv)]
         [:td (:current-usages inv)]
         [:td
          [:button {:class "button is-danger"} "Revoke"]]])]]]])

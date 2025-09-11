(ns galt.invitations.adapters.presentation.invitations-dashboard)

(defn present
  [{:keys [requests invitations]}]
  [:div {:class "container"}
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

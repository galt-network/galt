(ns galt.invitations.adapters.presentation.invitations-dashboard
  (:require
    [galt.core.adapters.time-helpers :as th]))

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

(defn invitation-requests-section
  [requests]
  [:section {:class "section"}
    [:h1 {:class "title"} "Invitation Requests"]
    [:table {:class "table is-striped is-hoverable is-fullwidth"}
     [:thead
      [:tr
       [:th "Requesting User"]
       [:th "To Member"]
       [:th "To Group"]
       [:th "Created At"]
       [:th "Actions"]]]
     [:tbody
      (for [req requests]
        [:tr
         [:td (:requesting-user req)]
         [:td (:to-member req)]
         [:td (:to-group req)]
         [:td (th/relative-time (:created-at req))]
         [:td
          [:div.buttons
           [:button {:class "button is-success"} "Approve"]
           [:button {:class "button is-danger"} "Deny"]]]])]]])

(defn active-invitations [invitations]
  [:section {:class "section"}
    [:h1 {:class "title"} "Active Invitations"]
    [:table {:class "table is-striped is-hoverable is-fullwidth"}
     [:thead
      [:tr
       [:th "Inviting Member"]
       [:th "To Group"]
       [:th "Created At"]
       [:th "Expires At"]
       [:th "Max Usages"]
       [:th "Current Usages"]
       [:th "Actions"]]]
     [:tbody
      (for [inv invitations]
        [:tr
         [:td (:inviting-member inv)]
         [:td (:to-group inv)]
         [:td (th/short-format (:created-at inv))]
         [:td (th/relative-time (:expires-at inv))]
         [:td.has-text-centered (:max-usages inv)]
         [:td.has-text-centered (:current-usages inv)]
         [:td
          [:div.buttons
           [:a {:class "button is-info" :href (:href inv) :target "_blank"} "View"]
           [:button {:class "button is-danger"} "Revoke"]]]])]]])

(defn inactive-invitations [invitations]
  [:section {:class "section"}
    [:h1 {:class "title"} "Past and Used Invitations"]
    [:table {:class "table is-striped is-hoverable is-fullwidth"}
     [:thead
      [:tr
       [:th "Inviting Member"]
       [:th "To Group"]
       [:th "Created At"]
       [:th "Expires At"]
       [:th "Max Usages"]
       [:th "Current Usages"]]]
     [:tbody
      (for [inv invitations]
        [:tr
         [:td (:inviting-member inv)]
         [:td (:to-group inv)]
         [:td (th/short-format (:created-at inv))]
         [:td (th/relative-time (:expires-at inv))]
         [:td.has-text-centered (:max-usages inv)]
         [:td.has-text-centered (:current-usages inv)]])]]])

(defn present
  [{:keys [requests active inactive]}]
  [:div {:class "container"}
   (new-invitation-section)
   (invitation-requests-section requests)
   (active-invitations active)
   (inactive-invitations inactive)])

(ns galt.members.adapters.presentation.profile
  (:require
    [galt.core.adapters.presentation-helpers :refer [render-markdown]]))

(defn present
  [{:keys [edit-href] :as model}]
  [:div
   [:section.hero
    [:div.hero-body
     [:div.container
      [:div.columns.is-vcentered
       [:div.column.is-narrow
        [:figure.image.is-128x128
         [:img.is-rounded {:src (:avatar model) :alt "Avatar"}]]]
       [:div.column
        [:h1.title.is-1 (:name model)]]
       [:div.column.is-one-fifth
        (when edit-href [:a.button {:href edit-href}
          [:span.icon [:i.fa-solid.fa-user-pen]]
          [:span "Edit"]])]
       ]]]]

   [:div.container.mt-6
    [:section.section
     [:h3.title.is-3 "Description"]
     [:div.content (render-markdown (:description model))]]
    [:section.section
     [:h2.title.is-3 "Groups"]
     [:div.columns
      [:div.column
       (if (empty? (:groups model))
         [:p "No groups joined yet."]
         [:ul (map (fn [group] [:li group]) (:groups model))])]]]
    (when (:location model)
      [:section.section
       [:h2.title.is-3 "Location"]
       [:div.columns
        [:div.column
         [:p (:location-name model)]]
        [:div.column
         [:div {:style "height: 400px; background-color: #f0f0f0; display: flex; align-items: center; justify-content: center; border: 1px solid #dbdbdb; border-radius: 6px;"}
          [:p.has-text-grey "Map Placeholder (Coordinates: " (str (:coords (:location model)) ")")]]]]])
    [:section.section
     [:h2.title.is-3 "Recent Activity"]
     [:div.columns.is-multiline
      [:div.column.is-half
       [:div.card
        [:div.card-header
         [:p.card-header-title "Comments on Posts"]]
        [:div.card-content
         (if (empty? (:recent-comments model))
           [:p "No recent comments."]
           [:ul
            (map (fn [comment]
                   [:li comment]) (:recent-comments model))])]]]
      [:div.column.is-half
       [:div.card
        [:div.card-header
         [:p.card-header-title "Unread Group Messages"]]
        [:div.card-content
         (if (empty? (:unread-messages model))
           [:p "No unread messages."]
           [:ul
            (map (fn [msg]
                   [:li msg]) (:unread-messages model))])]]]
      [:div.column.is-half
       [:div.card
        [:div.card-header
         [:p.card-header-title "Membership Invitations"]]
        [:div.card-content
         (if (empty? (:invitations model))
           [:p "No pending invitations."]
           [:ul
            (map (fn [inv]
                   [:li inv]) (:invitations model))])]]]
      [:div.column.is-half
       [:div.card
        [:div.card-header
         [:p.card-header-title "Received Messages"]]
        [:div.card-content
         (if (empty? (:received-messages model))
           [:p "No received messages."]
           [:ul
            (map (fn [msg] [:li msg]) (:received-messages model))])]]]]]]])

(defn present-error
  [errors]
  [:article.message.is-danger
     [:div.message-header [:p "Error in showing profile"]]
     [:div.message-body [:ul (map (fn [e] [:li e]) errors)]]])

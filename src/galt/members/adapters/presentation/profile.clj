(ns galt.members.adapters.presentation.profile
  (:require
    [galt.core.adapters.presentation-helpers :refer [render-markdown]]))

(defn present
  [model]
  (println ">>> PRESENT" model)
  (let [member (merge
                 {:name "John Doe"
                  :avatar "https://example.com/avatar.jpg"
                  :groups ["Group A" "Group B"]
                  :location {:coords [37.7749 -122.4194] :city "San Francisco" :country "USA"}
                  :recent-comments ["Comment 1" "Comment 2"]
                  :unread-messages ["Message from Group A"]
                  :invitations ["Invite to Group C"]
                  :received-messages ["Private message 1"]}
                 model)]
   [:div
    [:section.hero.is-warning
     [:div.hero-body
      [:div.container
       [:div.columns.is-vcentered
        [:div.column.is-narrow
         [:figure.image.is-128x128
          [:img.is-rounded {:src (:avatar member) :alt "Avatar"}]]]
        [:div.column
         [:h1.title.is-1 (:name member)]
         ]]]]]

    [:div.container.mt-6
     [:section.section
      [:h3.title.is-3 "Description"]
      [:div.content (render-markdown (:description model))]]
     [:section.section
      [:h2.title.is-3 "Groups"]
      [:div.columns
       [:div.column
        (if (empty? (:groups member))
          [:p "No groups joined yet."]
          [:ul (map (fn [group] [:li group]) (:groups member))])]]]
     (when (:location member)
       [:section.section
        [:h2.title.is-3 "Location"]
        [:div.columns
         [:div.column
          [:p (:location-name model)]]
         [:div.column
          [:div {:style "height: 400px; background-color: #f0f0f0; display: flex; align-items: center; justify-content: center; border: 1px solid #dbdbdb; border-radius: 6px;"}
           [:p.has-text-grey "Map Placeholder (Coordinates: " (str (:coords (:location member)) ")")]]]]])
     [:section.section
      [:h2.title.is-3 "Recent Activity"]
      [:div.columns.is-multiline
       [:div.column.is-half
        [:div.card
         [:div.card-header
          [:p.card-header-title "Comments on Posts"]]
         [:div.card-content
          (if (empty? (:recent-comments member))
            [:p "No recent comments."]
            [:ul
             (map (fn [comment]
                    [:li comment]) (:recent-comments member))])]]]
       [:div.column.is-half
        [:div.card
         [:div.card-header
          [:p.card-header-title "Unread Group Messages"]]
         [:div.card-content
          (if (empty? (:unread-messages member))
            [:p "No unread messages."]
            [:ul
             (map (fn [msg]
                    [:li msg]) (:unread-messages member))])]]]
       [:div.column.is-half
        [:div.card
         [:div.card-header
          [:p.card-header-title "Membership Invitations"]]
         [:div.card-content
          (if (empty? (:invitations member))
            [:p "No pending invitations."]
            [:ul
             (map (fn [inv]
                    [:li inv]) (:invitations member))])]]]
       [:div.column.is-half
        [:div.card
         [:div.card-header
          [:p.card-header-title "Received Messages"]]
         [:div.card-content
          (if (empty? (:received-messages member))
            [:p "No received messages."]
            [:ul
             (map (fn [msg] [:li msg]) (:received-messages member))])]]]]]]]))

(defn present-error
  [errors]
  [:article.message.is-danger
     [:div.message-header [:p "Error in showing profile"]]
     [:div.message-body [:ul (map (fn [e] [:li e]) errors)]]])

(ns galt.events.adapters.presentation.list-events
  (:require
   [galt.core.adapters.presentation-helpers :refer [render-markdown]]
   [galt.core.adapters.time-helpers :refer [relative-with-short]]))

(defn event-card
  [event]
  [:div.card
   [:div {:class "card-content"}
    [:div {:class "media"}
     [:div {:class "media-left"}
      [:figure {:class "image is-48x48"}
       [:img {:src (or (:author-avatar event) "https://bulma.io/assets/images/placeholders/96x96.png")
              :alt "Placeholder image"}]]]
     [:div {:class "media-content"}
      [:p {:class "title is-4"} (:author event)]]]
    [:div {:class "content"}
     (render-markdown (:description event))
     [:br]
     [:div
      "Published "
      [:time (relative-with-short (:publish-at event))]
      " by " (:author event)]]]])

(defn present
  [model]
  [:div
   [:h1.title.is-1 "Events"]
   [:a.button.is-fullwidth {:href (:new-event-href model)}
    [:span.icon [:i.fas.fa-calendar]]
    [:span "Create new event"]]
   (for [event (:events model)] (event-card event))])

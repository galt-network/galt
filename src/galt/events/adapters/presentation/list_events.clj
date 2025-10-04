(ns galt.events.adapters.presentation.list-events
  (:require
   [galt.core.adapters.time-helpers :as th :refer [relative-with-short]]
   ))

(defn event-card
  [event]
  [:div.card.hoverable-row {:onclick (str "location.href = '" (:event-link event) "'")}
   [:div.card-content
    [:div.media
     [:div.media-left
      [:figure.image.is-48x48
       [:img {:src (or (:author-avatar event) "https://bulma.io/assets/images/placeholders/96x96.png")
              :alt "Event organizer"}]]]
     [:div.media-content
      [:div.is-flex.is-justify-content-space-between
       [:p.title.is-4 [:a {:href (:event-link event)} (:name event)]]
       [:div.tags.are-medium
        [:span.tag
         [:i.fas.fa-clock {:style {:margin-right "0.5em"}}]
         (th/long-format-with-time (:start-time event))]
        [:span.tag (:type event)]]]]]
    [:div
     [:time "Created " (relative-with-short (:publish-at event))]
     " by " [:a {:href (str "/members/" (:author-id event))} (:author event)]]
    ]])

(defn present
  [model]
  [:div
   [:div.columns
    [:div.column
     [:h1.title.is-1 "Events"]]
    [:div.column.is-one-fifth
     [:a.button {:href (:new-event-href model)}
      [:span.icon [:i.fas.fa-calendar]]
      [:span "Create new event"]]]]
   [:div.columns
    [:input {:type "hidden" :name "offset" :value (:offset model) :data-bind "offset"}]
    [:input {:type "hidden" :name "limit" :value (:limit model) :data-bind "limit"}]
    [:div.column
     [:div.field
      [:label.label "Date range:"]
      [:div.control
       [:div.select
        [:select {:name "period" :data-on-change "@get('/events?patch-mode=inner')" :data-bind "period"}
         [:option {:value "today"} "Today"]
         [:option {:value "tomorrow"} "Tomorrow"]
         [:option {:value "this-week" :selected true} "This week"]
         [:option {:value "this-weekend"} "This weekend"]
         [:option {:value "next-week"} "Next week"]
         [:option {:value "all"} "All"]]]]]]
    [:div.column
     [:div.field
      [:label.label "Type"]
      [:div.control
       [:div.radios
        [:label.radio
         [:input {:name "type"
                  :type "radio"
                  :value "live"
                  :data-on-change "@get('/events?patch-mode=inner')"
                  :data-bind "type"}]
         "Live"]
        [:label.radio
         [:input {:name "type"
                  :type "radio"
                  :value "online"
                  :data-on-change "@get('/events?patch-mode=inner')"
                  :data-bind "type"
                  }]
         "Online"]]]]]]
   [:div {:id "event-cards" :data-signals (:initial-signals model)}
    (map event-card (:events model))]
   [:div {:data-on-intersect "@get('/events?patch-mode=append')"}]])

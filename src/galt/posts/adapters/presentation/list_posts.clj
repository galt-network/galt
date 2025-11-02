(ns galt.posts.adapters.presentation.list-posts
  (:require
    [galt.core.adapters.time-helpers :as th :refer [relative-with-short]]
    [galt.groups.adapters.presentation.show-group :refer [activity-card]]))

(defn post-card
  [post]
  [:article.media
   [:div.media-content
    [:div.columns.is-vcentered
     [:div.column.is-narrow
      [:figure.image.is-48x48
       [:img {:src (or (:author-avatar post) "https://bulma.io/assets/images/placeholders/96x96.png")
              :alt "Post author"}]]]
     [:div.column
      [:span.title.is-4 [:a {:href (:post-link post)} (:title post)]]]]
    [:div.tags.are-medium.mx-3
     [:span.tag
      [:i.fas.fa-clock {:style {:margin-right "0.5em"}}]
      (th/long-format-with-time (:publish-at post))]]
    [:div.mx-3
     "By " [:a {:href (str "/members/" (:author-id post))} (:author post)]]]])

(defn present
  [model]
  [:div
   [:div.columns
    [:div.column.is-one-fifth
     [:a.button {:href (:new-post-href model)}
      [:span.icon [:i.fas.fa-calendar]]
      [:span "New post"]]]]
   [:div.columns
    [:input {:type "hidden" :name "offset" :value (:offset model) :data-bind "offset"}]
    [:input {:type "hidden" :name "limit" :value (:limit model) :data-bind "limit"}]
    [:div.column
     [:div.field
      [:label.label "Date range:"]
      [:div.control
       [:div.select
        [:select {:name "period" :data-on:change "@get('/posts?patch-mode=inner')" :data-bind "period"}
         [:option {:value "today"} "Today"]
         [:option {:value "this-week" :selected true} "This week"]
         [:option {:value "this-month"} "This month"]
         [:option {:value "this-year"} "This year"]
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
                  :data-on:change "@get('/posts?patch-mode=inner')"
                  :data-bind "type"}]
         "Live"]
        [:label.radio
         [:input {:name "type"
                  :type "radio"
                  :value "online"
                  :data-on:change "@get('/posts?patch-mode=inner')"
                  :data-bind "type"
                  }]
         "Online"]]]]]]
   [:div {:id "event-cards" :data-signals (:initial-signals model)}
    (map post-card (:posts model))]
   [:div {:data-on-intersect "@get('/posts?patch-mode=append')"}]])

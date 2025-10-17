(ns galt.groups.adapters.presentation.list-groups
  (:require
    [galt.core.views.datastar-helpers :refer [d*-backend-action js-literal]]
    [galt.core.adapters.time-helpers :as th :refer [relative-with-short]]))

(defn group-row
  [group]
  [:article.media
   [:div.media-content
    [:div.columns.is-vcentered
     [:div.column.is-narrow
      [:figure.image.is-48x48
       [:img {:src (or (:avatar group) "https://bulma.io/assets/images/placeholders/96x96.png")
              :alt "Group logo"}]]]
     [:div.column
      [:span.title.is-4 [:a {:href (:group-link group)} (:name group)]]]]
    [:div.tags.are-medium.mx-3
     [:span.tag
      [:i.fas.fa-location-dot {:style {:margin-right "0.5em"}}]
      (get-in group [:location :name])
      ]
     [:span.tag (get-in group [:location :country-code])]]
    [:div.mx-3
     [:time "Founded " (relative-with-short (:created-at group))]]]])

(defn present
  [model]
  [:div
   [:div.columns
    [:div.column
     ; [:h1.title.is-1 "Groups"]
     [:div.panel-block
      [:div.field.has-addons
       [:p {:class [:control :has-icons-left]}
        [:input.input.is-fullwidth {:placeholder "Search"
                                    :name "query"
                                    :data-bind "query"
                                    :data-on-keyup "evt.keyCode === 13 && @get('/groups?patch-mode=inner')"}]
        [:span {:class "icon is-left"}
         [:i {:class "fas fa-search" :aria-hidden "true"}]]]
       [:p.control [:button.button.is-primary {:data-on-click (d*-backend-action "/members")} "Search"]]]]]
    [:div.column.is-one-fifth
     [:a.button {:href (:new-group-href model)}
      [:span.icon [:i.fas.fa-calendar]]
      [:span "Create new group"]]]]
   [:div.columns
    [:input {:type "hidden" :name "offset" :value (:offset model) :data-bind "offset"}]
    [:input {:type "hidden" :name "limit" :value (:limit model) :data-bind "limit"}]]
   [:div {:id "group-rows" :data-signals (:initial-signals model)}
    (map group-row (:groups model))]
   [:div {:data-on-intersect "@get('/groups?patch-mode=append')"}]])

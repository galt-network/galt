(ns galt.groups.adapters.presentation.show-group
  (:require
    [galt.core.adapters.time-helpers :refer [relative-with-short]]
    [galt.core.adapters.presentation-helpers :refer [render-markdown]]))

(defn activity-card
  [activity]
  [:div.card
   [:div {:class "card-content"}
    [:div {:class "media"}
     [:div {:class "media-left"}
      [:figure {:class "image is-48x48"}
       [:img {:src (or (:author-avatar activity) "https://bulma.io/assets/images/placeholders/96x96.png")
              :alt "Placeholder image"}]]]
     [:div {:class "media-content"}
      [:p {:class "title is-4"} (:author activity)]
      [:p {:class "subtitle is-6"} (:slug activity)]]]
    [:div {:class "content"}
     (render-markdown (:content activity))
     [:br]
     [:div
      "Published "
      [:time (relative-with-short (:publish-at activity))]
      " by " (:author activity)]]]])

(defn button
  [{:keys [href title]}]
  [:a.button.is-fullwidth {:href href}
   [:span.icon [:i.fas.fa-calendar]]
   [:span title]])

(defn present
  [model]
  [:div.section
   [:div.box
    [:div.columns.is-vcentered
     [:div.column.is-one-fifth
      [:figure.image.is-128x128
       [:img {:src (:avatar model)}]]]
     [:div.column [:h1.title.has-text-centered (:name model)]]
     [:div.column.is-one-fifth
      [:div.buttons (for [action (:actions model)] (button action))]]]
    [:div.columns
     [:div.column.is-one-third
      [:div.field [:label.label "Description"]
       [:div.control [:div.content (:description model)]]]
      [:div.field [:label.label "Language"]
       [:div.tags.are-medium
        (map (fn [lang] [:span.tag lang]) (:languages model))]]
      [:div.field [:label.label "Location"]
       [:div.control [:p (:location-name model)]]]
      [:div.field [:label.label "Founded At"]
       [:div.control [:p (:founded-at model)]]]
      [:div.field [:label.label "Members"]
       [:div.control
        [:ul
         (map (fn [member] [:a {:href (:href member)} [:li (:name member)]])
              (:members model))]]]]
     [:div.column
      [:div {:id "map"
             :style {:height "400px"}
             :data-on-load__delay.500ms
             (str "galtMoveMarker(" (:latitude model)  "," (:longitude model) ")")}]]]]
   [:div
    [:h2.is-size-3 "Recent activity"]
    (map activity-card (:activity model))]])

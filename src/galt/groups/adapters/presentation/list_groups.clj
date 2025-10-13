(ns galt.groups.adapters.presentation.list-groups
  (:require
   [galt.core.views.components.dropdown-search :as dropdown-search]
   [galt.core.views.table :refer [table]]))

(defn present
  [model]
  [:div
   (dropdown-search/dropdown-search {:name "group-name"
                                     :value (:group-name model)
                                     :id (:group-name-id model)
                                     :endpoint "/groups/search"})
   [:a.button.is-primary {:href "/groups/new"} "Create group"]
   (for [group (:groups model)]
     [:div.card
      [:div.card-content
       [:div.media
        [:div.media-left
         [:figure.image.is-128x128
          [:img.is-rounded {:src (:avatar group)}]]]
        [:div.media-content
         [:h3.title.is-4 (:name group)]
         [:p "Location: " (:location group)]]]

       ]])
   ; (table {:columns (:columns model),
   ;         :column-processor
   ;           {:actions (fn [actions]
   ;                       (map (fn [a] [:div.control
   ;                                     [:a.button {:href (:href a)} (:name a)]])
   ;                         actions)),
   ;            :after-actions (fn [actions] [:div.field.is-grouped actions])},
   ;         :rows (:groups model)})
   ])




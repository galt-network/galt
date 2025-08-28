(ns galt.groups.adapters.views
  (:require
    [galt.core.views.table :refer [table]]))

(defn groups-list
  [model]
  [:div
   [:button.button.is-primary {:data-on-click "@get('/groups/new')"} "Create group"]
   ; [:a.button.is-primary {:href "/groups/new"} "Create group"]
   (table {:columns (:columns model)
           :column-processor {:actions (fn [actions]
                                         (map (fn [a]
                                                [:div.control [:button.button (:action a) (:name a)]])
                                              actions))
                              :after-actions (fn [actions] [:div.field.is-grouped actions])}
           :rows (:groups model)})])

(defn group-form
  [_model]
  [:form {:onsubmit "return false;" :id "group-form" :enctype "multipart/form-data" :data-signals-files "[]"}
   [:div.field
    [:label.label "Group name"]
    [:div.control
     [:input#group-name.input {:type :text :data-bind "group-name"}]]]
   [:div.field
    [:label.label "Avatar"]
    [:div.control
     [:div.file.has-name
      [:label.file-label
       [:input.file-input {:type "file"
                           :name "uploaded-file"
                           :data-bind "files"
                           :data-on-change "$files && @post('/files', {contentType: 'form'})"}]
       [:span.file-cta
        [:span.file-icon
         [:i.fas.fa-upload]]
        [:span.file-label "Choose a file..."]]
       [:span.file-name {:data-text "$files[0].filename"}]]]]
    [:figure.image.is-128x128
     [:img {:src "https://bulma.io/assets/images/placeholders/128x128.png"
            :data-attr-src "$uploaded-url"}]]]
   [:div.field
    [:label.label "File info"]
    [:div.control
     [:input#group-avatar.input {:type :text :data-bind "uploaded-url"}]]]
   [:div.field
    [:label.label "Group description"]
    [:div.control
     [:textarea#group-description.textarea {:data-bind :group-description}]]]
   [:button.button.is-primary
    {:data-on-click "@post('/groups', {filterSignals: {exclude: /^files/}})"} "Create!"]])

(defn create-group-form
  [model]
  [:div.box
   [:div.block
    [:h2.is-size-2 "Create a new group"]
    (group-form model)]])

(defn show-group
  [model]
  [:div
   [:div.box
    [:h1.title (:name model)]
    [:div.field
     [:label.label "Description"]
     [:div.control
      [:div.content (:description model)]]]
    [:div.field
     [:label.label "Language"]
     [:div.tags.are-medium
      (map (fn [lang] [:span.tag lang]) (:languages model))
      ]]
    [:div.field
     [:label.label "Location"]
     [:div.control
      [:p (:location-name model)]]]
    [:div.field
     [:label.label "Founded At"]
     [:div.control
      [:p (:founded-at model)]]]
    [:div.field
     [:label.label "Members"]
     [:div.control
      [:ul
       (map (fn [member] [:a {:href (:href member)} [:li (:name member)]]) (:members model))]]]]])

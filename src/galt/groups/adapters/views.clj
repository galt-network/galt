(ns galt.groups.adapters.views
  (:require
    [galt.core.views.table :refer [table]]
    [clojure.string :as str]
    [galt.locations.adapters.presentation :as location-views]))

(defn groups-list
  [model]
  [:div [:a.button.is-primary {:href "/groups/new"} "Create group"]
   (table {:columns (:columns model),
           :column-processor
             {:actions (fn [actions]
                         (map (fn [a] [:div.control
                                       [:a.button {:href (:href a)} (:name a)]])
                           actions)),
              :after-actions (fn [actions] [:div.field.is-grouped actions])},
           :rows (:groups model)})])

(defn hidden-form-method
  [method]
  (let [allowed-methods #{"GET" "PUT" "POST" "DELETE" "PATCH"}
        normalized-method (str/upper-case (name (or method :post)))]
    (if (allowed-methods normalized-method)
      [:input {:type "hidden" :name "_method" :value normalized-method}]
      (throw (Exception. (str "Unrecognized form method: " method))))))

(defn group-form
  [{:keys [form countries group location]}]
  [:form
   {:id "group-form",
    :enctype "multipart/form-data",
    :action (:action-target form),
    :method "POST",
    :data-signals-files "[]"
    :onkeydown "if (event.keyCode === 13 && event.target.type !== 'textarea') { event.preventDefault(); }"}
   (hidden-form-method (:action-method form))
   [:input
    {:type "hidden",
     :name "uploaded-url",
     :data-attr-value "$uploaded-url"}]
   [:div.field
    [:label.label {:for :group-name} "Group name"]
    [:div.control
     [:input#group-name.input
      {:type :text, :name :group-name, :value (:name group)}]]]
   [:div.columns
    [:div.column.is-one-third
     [:div.field
      [:label.label "Avatar"]
      [:div.control
       [:div.file
        [:label.file-label
         [:input.file-input
          {:type "file",
           :name "uploaded-file",
           :data-bind "files",
           :data-on-change "$files && @post('/files', {contentType: 'form'})"
           }]
         [:span.file-cta
          [:span.file-icon [:i.fas.fa-upload]]
          [:span.file-label "Choose a file..."]]]]]
      [:figure.image.is-128x128
       [:img#group-avatar
        {:src (or (:avatar group) "/assets/images/avatar-128x128.png")
         :data-attr-src "$uploaded-url"
         :data-show "$uploaded-url"
         :data-on-load__.delay.500ms (str "$uploaded-url = '" (:avatar group) "'")}]]]]
    [:div.column (when location
                   {:data-on-load__delay.500ms
                    (str "galtMoveMarker(" (:latitude location)  "," (:longitude location) ")")})
     (location-views/searchable-map {:countries countries
                                     :location location})]]
   [:div.field
    [:label.label {:for :group-description} "Group description"]
    [:div.control
     [:textarea#group-description.textarea {:name :group-description}
      (:description group)]]]
   [:button.button.is-primary (:action-name form)]])

(defn new-group
  [model]
  [:div.box
   [:div.block
    [:h2.is-size-2 "Create a new group"]
    (group-form model)]])

(defn delete-modal
  [{:keys [delete-action name]}]
  [:div {:class "modal" :data-class-is-active "$modal-show"}
   [:div {:class "modal-background"}]
   [:div {:class "modal-card"}
    [:header {:class "modal-card-head"}
     [:p {:class "modal-card-title"} "Are you sure?"]
     [:button {:class "delete" :aria-label "close" :data-on-click "$modal-show = false"}]]
    [:section {:class "modal-card-body"}
     [:div.content
      "You're about to delete the group "
      [:strong name]
      " This action is permanent. "
      "If this is what you want, please confirm it by clicking the [Confirm delete] button"]]
    [:footer {:class "modal-card-foot"}
     [:div {:class "buttons"}
      [:button {:class [:button :is-danger] :data-on-click delete-action}
       "Confirm delete"]
      [:button {:class [:button] :data-on-click "$modal-show = false"} "Cancel"]]]]])

(defn edit-group
  [{:keys [form] :as model}]
  [:div.box
   [:div.block
    [:h2.is-size-2 "Edit group"]
    (group-form model)
    [:div.control [:button.button.is-danger {:data-on-click "$modal-show = true"} "Delete group"]]]
   (delete-modal form)])

(defn show-group
  [model]
  [:div.box
   [:div.columns.is-vcentered
    [:div.column
     [:figure.image.is-128x128
      [:img {:src (:avatar model)}]]]
    [:div.column.is-four-fifths [:h1.title (:name model)]]]
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
            :data-on-load__delay.500ms (str "galtMoveMarker(" (:latitude model)  "," (:longitude model) ")")}]]]])

(defn error-messages
  [errors]
  [:article.message.is-danger
   [:div.message-header [:p "Errors in creating group"]]
   [:div.message-body [:ul (map (fn [e] [:li e]) errors)]]])

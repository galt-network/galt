(ns galt.members.adapters.presentation.edit-profile
  (:require
    [galt.locations.adapters.presentation :as location-views]
    [clojure.string :as str]))

(defn hidden-form-method
  [method]
  (let [allowed-methods #{"GET" "PUT" "POST" "DELETE" "PATCH"}
        normalized-method (str/upper-case (name (or method :post)))]
    (if (allowed-methods normalized-method)
      [:input {:type "hidden" :name "_method" :value normalized-method}]
      (throw (Exception. (str "Unrecognized form method: " method))))))

(defn present
  [{:keys [form countries member location]}]
  [:form
   {:id "member-form",
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
    [:label.label {:for :member-name} "Name"]
    [:div.control
     [:input#member-name.input
      {:type :text, :name :member-name, :value (:name member)}]]
    [:p "Will be shown in the content (posts, messages, etc.)"]]
   [:div.field
    [:label.label {:for :member-slug} "Slug"]
    [:div.control
     [:input#member-slug.input
      {:type :text, :name :member-slug, :value (:slug member)}]]
    [:p "Will be used in URL-s (e.g. human readable link to your profile galt.is/m/<slug>), links and to find you"]]
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
       [:img#member-avatar
        {:src (or (:avatar member) "/assets/images/avatar-128x128.png")
         :data-attr-src "$uploaded-url"
         :data-show "$uploaded-url"
         :data-on-load__delay.500ms (str "$uploaded-url = '" (:avatar member) "'")}]]]]
    [:div.column (when location
                   {:data-on-load__delay.500ms
                    (str "galtMoveMarker(" (:latitude location)  "," (:longitude location) ")")})
     (location-views/searchable-map {:countries countries
                                     :location location
                                     :output-params
                                     {:city-id "city-id"
                                     :country-code "country-code"
                                     :location-name "location-name"
                                     :latitude "latitude"
                                     :longitude "longitude"}})]]
   [:div.field
    [:label.label {:for :member-description} "Profile description"]
    [:div.control
     [:textarea#member-description.textarea {:name :member-description}
      (:description member)]]
    [:p "Can use Markdown"]]
   [:button.button.is-primary (:action-name form)]])

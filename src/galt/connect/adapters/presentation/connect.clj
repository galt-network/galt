(ns galt.connect.adapters.presentation.connect
  (:require
   [clojure.string :as str]
   [galt.core.views.datastar-helpers :refer [js-literal]]))

(defn- type-checked?
  [types t]
  (or (nil? types) (contains? types t)))

(defn near-me-notice
  [dropped?]
  (when dropped?
    [:p "Near me was dropped"]))

(defn hit-card
  [hit]
  [:article.media
   [:div.media-content
    [:span.tag.is-light (name (:type hit))]
    [:span.title.is-5.ml-2
     (if-let [href (:href hit)]
       [:a {:href href} (:title hit)]
       (:title hit))]
    (when-let [snippet (:snippet hit)]
      [:p snippet])]])

(def ^:private refetch-inner
  "$offset = 0; @get('/connect?patch-mode=inner')")

(def ^:private dismiss-location
  (str "$geo = 'off'; $lat = null; $lng = null; $label = ''; " refetch-inner))

(def ^:private near-me
  (str "$geo = ''; $lat = null; $lng = null; $label = ''; " refetch-inner))

(defn location-bar
  [location]
  [:div#connect-location
   (if location
     [:div
      [:input {:type "hidden" :name "lat" :value (:lat location) :data-bind "lat"}]
      [:input {:type "hidden" :name "lng" :value (:lng location) :data-bind "lng"}]
      [:input {:type "hidden" :name "label" :value (:label location) :data-bind "label"}]
      [:input {:type "hidden" :name "radius" :value (:radius-km location) :data-bind "radius"}]
      [:div.tags.are-medium.mt-2
       [:span.tag.is-info
        [:span.icon [:i.fas.fa-map-marker-alt]]
        [:span (:label location)]
        [:a.delete {:href "/connect?geo=off"
                    :data-on:click__prevent dismiss-location}]]]]
     [:div.mb-3
      [:a.button.is-light {:href "/connect"
                           :data-on:click__prevent near-me}
       [:span.icon [:i.fas.fa-location-arrow]]
       [:span "Near me"]]])])

(defn- type-flag-signals
  [types]
  (into {} (map (fn [t] [t (type-checked? types t)]) [:member :group :event :post])))

(defn- page-signals
  [model]
  (let [loc (:location model)]
    (js-literal (merge {:offset (or (:next-offset model) 10)
                        :limit (or (:limit model) 10)
                        :query (or (:query model) "")
                        :period (or (:period model) "all")
                        :lat (:lat loc)
                        :lng (:lng loc)
                        :label (or (:label loc) "")
                        :radius (or (:radius-km loc) 100)
                        :geo (if loc "" "off")}
                       (type-flag-signals (:types model))))))

(defn present
  [model]
  [:div#connect {:data-signals (page-signals model)}
   [:h1.title "Connect"]
   [:form {:action "/connect" :method "get"}
    (location-bar (:location model))
    [:div.panel-block
     [:div.field.has-addons
      [:p.control.has-icons-left
       [:input.input.is-fullwidth {:placeholder "Search"
                                   :name "query"
                                   :data-bind "query"
                                   :value (:query model)}]
       [:span.icon.is-left [:i.fas.fa-search {:aria-hidden "true"}]]]
      [:p.control
       [:button.button.is-primary {:type "submit"} "Search"]]]]
    [:div.field.is-grouped.is-grouped-multiline.mt-3
     (for [t [:member :group :event :post]]
       [:label.checkbox.mr-3
        [:input {:type "checkbox"
                 :name "types"
                 :value (name t)
                 :data-bind (name t)
                 :data-on:change refetch-inner
                 :checked (type-checked? (:types model) t)}]
        " " (str/capitalize (name t))])]
    [:div.field
     [:label.label "Date range"]
     [:div.select
      [:select {:name "period"
                :data-bind "period"
                :data-on:change refetch-inner}
       (for [[v label] [["all" "All"]
                        ["today" "Today"]
                        ["tomorrow" "Tomorrow"]
                        ["this-week" "This week"]
                        ["this-weekend" "This weekend"]
                        ["next-week" "Next week"]]
             :let [selected? (= (or (:period model) "all") v)]]
         [:option (cond-> {:value v} selected? (assoc :selected true)) label])]]]]
   [:div#connect-notice
    (near-me-notice (:near-me-dropped? model))]
   [:div#connect-hits
    (map hit-card (:hits model))]
   [:div {:data-on-intersect "@get('/connect?patch-mode=append')"}]
   [:p.is-size-7.has-text-grey.mt-4
    [:a {:href "https://db-ip.com/" :target "_blank" :rel "noopener"}
     "IP Geolocation by DB-IP"]]])

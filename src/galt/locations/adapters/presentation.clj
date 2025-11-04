(ns galt.locations.adapters.presentation
  (:require
    [galt.core.views.datastar-helpers :refer [d*-backend-action]]))

(defn dropdown-content
  [content]
  [:div.dropdown-content {:id "options-content"} content])

(defn country-option
  [c selected?]
  [:option {:value (:code c) :selected selected?}
   (str (:emoji c) " " (:name c))])

(defn dropdown-search
  [countries location output-params]
  [:div
   [:label.label "Country"]
   [:div.field.has-addons
    [:div.control
     [:div.select
      [:select {:name (:country-code output-params)
                :data-bind "country-code"
                :data-on:change (d*-backend-action "/locations/coordinates"
                                                   :get
                                                   {:type "country"}
                                                   {:filter-signals {:exclude "/files/"}})}
       [:option {:value ""} "Choose a country"]
       (map (fn [c] (country-option c (= (:code c) (:country-code location)))) countries)]]]
    [:div.control
     [:button.button.is-info {:onclick "return false;" :data-on:click "$country-code = undefined"}"Clear"]]]
   [:div.field {:style {:margin-bottom "12px"}}
    [:label.label "City"]
    [:div.control {:style {:z-index 1000}}
     [:div.dropdown {:id "city-dropdown"
                     :data-class:is-active "$show-results && !$final-value"
                     :style {:display "block"}}
      [:div.dropdown-trigger
       [:input.input {:placeholder "Type to search"
                      :value (:name location)
                      :data-bind "search"
                      :data-on:input__debounce.500ms
                      (d*-backend-action "/locations/search-cities"
                                         :get
                                         {}
                                         {:filter-signals {:include "/search|country-code/"}})
                      :data-on:focus "$show-results = true; $final-value = null"
                      :data-on:click__outside "$show-results = false"
                      }]]
      [:div.dropdown-menu {:id "dropdown-menu" :role "menu"}
       (dropdown-content (list))]]
     [:input {:type :hidden
              :id "city-id"
              :name (:city-id output-params)
              :data-bind "city-id"}]
     [:input {:type :hidden
              :name (:latitude output-params)
              :id "latitude"
              :data-bind "latitude"
              :value (:latitude location)}]
     [:input {:type :hidden
              :name (:longitude output-params)
              :id "longitude"
              :data-bind "longitude"
              :value (:longitude location)}]
     [:input {:type :hidden :name (:location-name output-params):id "location-name" :data-attr "{value: $search}"}]]]])

(defn dropdown-item
  [{:keys [name extra value]}]
  [:div.dropdown-item {:data-on:click
                       (str "$search = '" name "'; $show-results = false;"
                            (d*-backend-action "/locations/coordinates"
                                               :get
                                               {:type "city" :id value}
                                               {:filter-signals {:exclude "/files/"}}))}
   name
   [:span.is-pulled-right {:style {:margin-left "1em"}} extra]])

(defn searchable-map
  [{:keys [countries location output-params]}]
  [:div
   (dropdown-search countries location output-params)
   [:div.field
    [:div {:id "map"
           :style {:height "400px"}
           :data-effect "if (typeof galtMoveMarker !== 'undefined' && ($latitude && $longitude)) { galtMoveMarker($latitude, $longitude) }"
           :data-on:update-coordinates "$latitude = evt.detail[0]; $longitude = evt.detail[1]"}]]])

(ns galt.core.views.components.dropdown-search
  (:require
    [galt.core.views.datastar-helpers :refer [d*-backend-action]]))

(defn dropdown-search-menu-item
  [name endpoint {:keys [value id extra]}]
  [:div.dropdown-item {:data-on:click
                       (d*-backend-action endpoint
                                          :get
                                          {:action "choose" :name name :value value :id id})}
   value
   (when extra [:span.is-pulled-right {:style {:margin-left "1em"}} extra])])

(defn dropdown-search-menu
  [name endpoint items]
  [:div.dropdown-menu {:id (str name "-search-dropdown-container")}
   (when (seq items) [:div.dropdown-content
                      (map (partial dropdown-search-menu-item name endpoint) items)])])

(defn id-element-name [search-element-name]
  (str (name search-element-name) "-id"))

(defn extra-signal-name [extra-signal name]
  (str name "-" extra-signal))

(defn show-results-signal-name
  [search-element-name]
  (str search-element-name "-show-results"))

(defn dropdown-search
  "Presents a search box with auto-complete dropdown of search results.
  To be included in an existing form.

  Arguments are in a map with the following keyword keys:
    :endpoint     - URL of the endpoint that datastar will make SSE request
                    This endpoint responds with HTML data via SSE that gets
                    inserted into DOM to display the results
    :name         - Name of the hidden form tag that will have the final result
    :value        - Initial value of the search text-box
    :id           - Value of id property (under <name>-id) of the searched entity
  "
  [{:keys [name value id endpoint extra-signal] :or {name "group-name" value "Galt" id "0ab34-ef87444"}}]
  [:div.dropdown {:data-class:is-active (str "$" (show-results-signal-name name))}
   [:div.dropdown-trigger
    [:input.input {:name name
                   :value value
                   :data-bind name
                   :data-on:focus (str "$" (show-results-signal-name name) " = true")
                   :data-on:click__outside__delay.10ms (str "$" (show-results-signal-name name) " = false")
                   :data-on:input__debounce.500ms
                   (d*-backend-action endpoint
                                      :get
                                      {:action "search" :search-signal-name name :extra-signal-name extra-signal}
                                      {:filter-signals
                                       {:include (str "/" name  "|" extra-signal "/")}})}]
    [:input {:type "hidden" :name (id-element-name name) :value id :data-bind (id-element-name name)}]]
   (dropdown-search-menu name endpoint (list))])

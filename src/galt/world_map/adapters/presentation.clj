(ns galt.world-map.adapters.presentation
  (:require
   [galt.core.views.layout :as layout]
   [hiccup2.core]))

(defn- notification-container
  []
  [:div#notification-container
   [:div {:class [:notification :is-success]
          :data-class:is-visible "$notification-visible"
          :data-class:is-danger "$notification-is-danger"
          :data-class:is-success "$notification-is-success"}
    [:button.delete {:data-on:click "$notification-visible = false"}]
    [:p {:data-text "$notification-text"}]]])

(defn world-map-layout
  "Full-bleed page layout for the world map: sliding navbar on top of a
  full-viewport globe, no footer."
  [mount-path model]
  [:html
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    [:title (str (:page-title model) " | Galt")]
    [:link {:rel "stylesheet" :href "https://cdn.jsdelivr.net/npm/bulma@1.0.4/css/bulma.min.css"}]
    [:link {:rel "stylesheet" :href "/assets/css/style.css"}]
    [:link {:rel "stylesheet" :href (str mount-path "/assets/css/main.css")}]
    [:link {:rel "stylesheet" :href "/assets/css/world-map.css"}]
    [:script {:type :module
              :src "https://cdn.jsdelivr.net/gh/starfederation/datastar@v1.0.0-RC.6/bundles/datastar.js"}]
    [:script {:src "https://kit.fontawesome.com/cb70718952.js" :crossorigin "anonymous"}]
    [:script {:src "https://cdn.jsdelivr.net/npm/scittle@0.7.27/dist/scittle.js"}]
    [:script {:src "https://cdn.jsdelivr.net/npm/scittle@0.7.27/dist/scittle.nrepl.js"}]
    [:script "var SCITTLE_NREPL_WEBSOCKET_PORT = 1340;"]
    [:script {:src "/assets/galt_js_helpers.js"}]
    [:script {:src "//cdn.jsdelivr.net/npm/globe.gl"}]
    (reverse (into (list) (:head-tags model)))]
   [:body
     [:div#world-map-page
      [:div#world-map-navbar-wrap
       [:div#world-map-navbar
        (layout/navbar (:navbar model))]
       [:div#world-map-handle
        [:i.fa.fa-solid.fa-chevron-down]]]
      [:div#app]
      (notification-container)]
     [:script {:src "/assets/js/world-map.js"}]
    [:script {:src (str mount-path "/assets/js/globo.js")}]
    [:script (hiccup2.core/raw
              (str "is.galt.globo.ui.init({\"globo-api-base-url\": \"" mount-path "\"})"))]]])

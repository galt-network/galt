(ns galt-promo.ui
  (:require [galt-promo.events :as events]
            [galt-promo.globe :as globe]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [reagent.dom :as dom]))

(defonce event-source (atom nil))
(defonce browser-location (atom nil))
(defonce location-requested? (atom false))
(defonce globe-controller (atom nil))
(defonce latest-users (atom []))

(declare request-location! globe-component client-id)

(defn- globe-node [node]
  (if node
    (when-not @globe-controller
      (reset! globe-controller
              (globe/mount! node @latest-users (client-id)
                            #(rf/dispatch [::events/cluster-selected %]))))
    (when-let [controller @globe-controller]
      (globe/destroy! controller)
      (reset! globe-controller nil))))

(def browser-icons
  {"Chrome" "🌐"
   "Edge" "🌊"
   "Firefox" "🦊"
   "Safari" "🧭"
   "Other" "🌐"})

(defn- user-row [{:keys [id name browser ip connected?]}]
  [:tr {:key id}
   [:td name]
   [:td
    [:span.tag.is-light
     [:span.mr-1 {:aria-hidden true} (get browser-icons browser "🌐")]
     [:span browser]]]
   [:td [:span.tag.is-info.is-light ip]]
   [:td
    [:span.status-dot
     {:class (if connected? "is-connected" "is-disconnected")
      :role "img"
      :aria-label (if connected? "Connected" "Disconnected")
      :title (if connected? "Connected" "Disconnected")}]]])

(defn- tab-icon [kind]
  (case kind
    :list
    [:svg {:width 16 :height 16 :viewBox "0 0 24 24"
           :fill "none" :stroke "currentColor" :stroke-width 2
           :stroke-linecap "round" :aria-hidden true}
     [:path {:d "M4 6h16M4 12h16M4 18h16"}]]
    :map
    [:svg {:width 16 :height 16 :viewBox "0 0 24 24"
           :fill "currentColor" :aria-hidden true}
     [:path {:d "M12 2a7 7 0 0 0-7 7c0 5 7 13 7 13s7-8 7-13a7 7 0 0 0-7-7zm0 9.5a2.5 2.5 0 1 1 0-5 2.5 2.5 0 0 1 0 5z"}]]))

(defn- select-tab! [tab]
  (rf/dispatch [::events/tab-selected tab])
  (when (= :map tab)
    (request-location!)))

(defn- tab-item [active? tab label icon]
  [:li {:class (when active? "is-active")}
   [:a {:href (str "#" (name tab))
        :aria-current (when active? "page")
        :on-click (fn [event]
                    (.preventDefault event)
                    (select-tab! tab))}
    [:span.icon.is-small (tab-icon icon)]
    [:span label]]])

(defn- selected-cluster-panel [users selected]
  (when (seq selected)
    (let [selected (set selected)
          members (filterv #(selected (:id %)) users)]
      (when (seq members)
        [:div.box.cluster-panel
         [:div.is-flex.is-justify-content-space-between.is-align-items-center.mb-3
          [:h2.subtitle.is-6.mb-0 (str (count members) " users here")]
          [:button.delete {:type "button"
                           :aria-label "Close cluster"
                           :on-click #(rf/dispatch [::events/cluster-closed])}]]
         [:div.cluster-list
          (for [{:keys [id name browser ip connected?]} members]
            [:div.cluster-row {:key id}
             [:span.has-text-weight-semibold name]
             [:span.tag.is-light
              [:span.mr-1 {:aria-hidden true} (get browser-icons browser "🌐")]
              [:span browser]]
             [:span.tag.is-info.is-light ip]
             [:span.status-dot
              {:class (if connected? "is-connected" "is-disconnected")
               :role "img"
               :aria-label (if connected? "Connected" "Disconnected")}]])]]))))

(defn- globe-view []
  [globe-component @(rf/subscribe [::events/users])])

(defonce globe-component
  (r/create-class
   {:display-name "GlobeView"
    :component-did-update
    (fn [this _]
      (when-let [controller @globe-controller]
        (globe/set-users! controller (second (r/argv this)))))
    :reagent-render
    (fn [users]
      (reset! latest-users users)
      [:div.globe-container {:ref globe-node}])}))

(defn- users-view [users]
  (if (seq users)
    [:div.table-container
     [:table.table.is-fullwidth.is-hoverable
      [:thead [:tr [:th "Name"] [:th "Browser"] [:th "IP"] [:th "Status"]]]
      [:tbody (map user-row users)]]]
    [:div.notification.is-light "Waiting for users…"]))

(defn- app []
  (let [users @(rf/subscribe [::events/users])
        connection @(rf/subscribe [::events/connection])
        tab @(rf/subscribe [::events/tab])
        selected @(rf/subscribe [::events/selected-cluster])]
    [:div.app-shell
     [globe-view]
     [:h1.app-brand "GALT - Global Alliance for Libertarian Transformation"]
     [:span.tag.connection-tag
      {:class (if (= :open connection) "is-success" "is-light")}
      (if (= :open connection) "Live" "Reconnecting")]
     [:nav.tabs.is-boxed.app-tabs {:aria-label "View"}
      [:ul
       [tab-item (= tab :list) :list "List" :list]
       [tab-item (= tab :map) :map "Map" :map]]]
     (if (= tab :map)
       [:<>
        [selected-cluster-panel users selected]
        [:p.map-help
         "Markers use precise browser location when permitted, otherwise IP-based location."]]
       [:main.users-overlay [users-view users]])]))

(defn- client-id []
  (or (.getItem js/localStorage "galt-promo-client-id")
      (let [id (.randomUUID js/crypto)]
        (.setItem js/localStorage "galt-promo-client-id" id)
        id)))

(defn- post-location! []
  (when-let [{:keys [lat lng]} @browser-location]
    (let [params (js/URLSearchParams.)]
      (.set params "client-id" (client-id))
      (.set params "lat" lat)
      (.set params "lng" lng)
      (-> (js/fetch (str "/location?" (.toString params)) #js {:method "POST"})
          (.catch (fn [_] nil))))))

(defn- request-location! []
  (when-not @location-requested?
    (reset! location-requested? true)
    (when-let [geolocation (.-geolocation js/navigator)]
      (.getCurrentPosition
       geolocation
       (fn [position]
         (let [coordinates (.-coords position)]
           (reset! browser-location {:lat (.-latitude coordinates)
                                     :lng (.-longitude coordinates)})
           (post-location!)))
       (fn [_] nil)
       #js {:enableHighAccuracy true
            :maximumAge 300000
            :timeout 10000}))))

(defn- connect! []
  (when-let [source @event-source]
    (.close source))
  (let [source (js/EventSource. (str "/connect?client-id=" (js/encodeURIComponent (client-id))))]
    (reset! event-source source)
    (set! (.-onopen source)
          #(do
             (rf/dispatch [::events/connection-changed :open])
             (post-location!)))
    (set! (.-onerror source) #(rf/dispatch [::events/connection-changed :reconnecting]))
    (.addEventListener source "users"
                       #(rf/dispatch [::events/users-received
                                      (js->clj (js/JSON.parse (.-data %)) :keywordize-keys true)]))))

(defn init []
  (rf/dispatch-sync [::events/initialize])
  (dom/render [app] (.getElementById js/document "app"))
  (connect!)
  (request-location!))

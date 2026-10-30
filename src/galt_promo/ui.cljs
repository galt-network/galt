(ns galt-promo.ui
  (:require [galt-promo.events :as events]
            [re-frame.core :as rf]
            [reagent.dom :as dom]))

(defonce event-source (atom nil))

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

(defn- app []
  (let [users @(rf/subscribe [::events/users])
        connection @(rf/subscribe [::events/connection])]
    [:section.section
     [:div.container
      [:h1.title "Galt Promo"]
      [:p.subtitle "Users connected over Server-Sent Events"]
      [:div.tags.mb-4
       [:span.tag {:class (if (= :open connection) "is-success" "is-light")}
        (if (= :open connection) "Live" "Reconnecting")]]
      (if (seq users)
        [:div.table-container
         [:table.table.is-fullwidth.is-hoverable
          [:thead [:tr [:th "Name"] [:th "Browser"] [:th "IP"] [:th "Status"]]]
          [:tbody (map user-row users)]]]
        [:div.notification.is-light "Waiting for users…"])]]))

(defn- client-id []
  (or (.getItem js/localStorage "galt-promo-client-id")
      (let [id (.randomUUID js/crypto)]
        (.setItem js/localStorage "galt-promo-client-id" id)
        id)))

(defn- connect! []
  (when-let [source @event-source]
    (.close source))
  (let [source (js/EventSource. (str "/connect?client-id=" (js/encodeURIComponent (client-id))))]
    (reset! event-source source)
    (set! (.-onopen source) #(rf/dispatch [::events/connection-changed :open]))
    (set! (.-onerror source) #(rf/dispatch [::events/connection-changed :reconnecting]))
    (.addEventListener source "users"
                       #(rf/dispatch [::events/users-received
                                      (js->clj (js/JSON.parse (.-data %)) :keywordize-keys true)]))))

(defn init []
  (rf/dispatch-sync [::events/initialize])
  (dom/render [app] (.getElementById js/document "app"))
  (connect!))

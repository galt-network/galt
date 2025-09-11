(ns galt.core.views.layout
  (:require
    [hiccup2.core]))

(def item-icons
  {:profile [:fa :fa-user-o]
   :invitations [:fa :fa-envelope-open-text]
   :logout [:fa :fa-right-from-bracket]
   :login [:fa :fa-right-to-bracket]})

(defn navbar-item
  [item]
  (let [icon-classes (item-icons (:id item))
        datastar-props (select-keys item [:data-on-click])
        class-props {:class (if (:selected? item) [:is-selected] [])}
        link-props {:href (:href item)}]
    [:a.navbar-item
     (merge datastar-props link-props class-props)
     (when icon-classes [:span.icon.is-small [:i {:class icon-classes}]])
     (:title item)]))

(defn navbar
  [model]
  [:nav.navbar {:id :navbar :role "navigation" :aria-label "main navigation"}
   [:div.navbar-brand
    [:a.navbar-item {:href "/"} "GALT"]]
   [:div.navbar-menu
    (into [:div.navbar-start] (map navbar-item (:items model)))
    [:div.navbar-end
     [:div.navbar-item {:class [:has-dropdown :is-hoverable]}
      [:a.navbar-link {:href (-> model :user :href)}
       [:figure.image.is-32x32
        [:img {:src (-> model :user :avatar)}]] (-> model :user :name)]
      (into [:div.navbar-dropdown.is-right] (map navbar-item (:dropdown model)))]]]])

(defn content
  [hiccup]
  [:div.container {:id :content} hiccup])

(defn app-container
  [model]
  [:div#app-container
   (navbar (model :navbar))
   [:section.is-medium {:style {:margin "3rem"} }
    (content (model :content))]
   [:div#notification-container
     [:div {:class [:notification :is-success]
            :data-class-is-visible "$notification-visible"
            :data-class-is-danger "$notification-is-danger"
            :data-class-is-success "$notification-is-success"}
      [:button.delete {:data-on-click "$notification-visible = false"}]
      [:p {:data-text "$notification-text"}]]]])

(def history-js
  "function galt_historyHandler(event) {
     let historyLink = document.getElementById('galt-history');
     if (event.state == null) {
       console.log('Not changing galt-history URL event.state = :', event.state);
     } else {
       historyLink.href = event.state.url;
     }
     console.log('history-js changed href to', historyLink.href);
     document.getElementById('galt-history').click();
    }
    console.log('Adding popstate listener', galt_historyHandler);
    window.addEventListener('popstate', galt_historyHandler);
    ")

(defn main-layout
  [model]
  [:html
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    [:title (str (:page-title model) " | Galt")]
    [:link {:rel "stylesheet" :href "https://cdn.jsdelivr.net/npm/bulma@1.0.4/css/bulma.min.css"}]
    [:link {:rel "stylesheet" :href "/assets/css/style.css"}]
    [:script {:type :module
              :src "https://cdn.jsdelivr.net/gh/starfederation/datastar@main/bundles/datastar.js"}]
    [:script {:src "https://kit.fontawesome.com/cb70718952.js" :crossorigin "anonymous"}]
    [:script {:src "https://cdn.jsdelivr.net/npm/scittle@0.7.27/dist/scittle.js"}]
    [:script {:src "https://cdn.jsdelivr.net/npm/scittle@0.7.27/dist/scittle.nrepl.js"}]
    [:script "var SCITTLE_NREPL_WEBSOCKET_PORT = 1340;"]
    [:script {:src "/assets/galt_js_helpers.js"}]
    (reverse (into (list) (:head-tags model)))]
   [:body
    (app-container model)
    [:a {:id "galt-history" :href (:path model) :data-on-click "@get(el.href)"}]
    [:script (hiccup2.core/raw history-js)]]])

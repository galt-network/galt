(ns galt.members.adapters.presentation.members-list
  (:require
   [galt.core.infrastructure.web.helpers :refer [render-html]]
   [galt.core.views.datastar-helpers :refer [d*-backend-action js-literal]]))

(defn group-tags
  [groups]
  (let [tags (map (fn [g] [:a {:href (:href g)} [:span.tag (:name g)]]) groups)
        groups-to-show 4
        remaining-count (- (count tags) groups-to-show)
        more-text (str "And " remaining-count " more...")]
    (if (> (count tags) groups-to-show)
      (conj [:div.tags] (take groups-to-show tags) [:span.tag more-text])
      [:div.tags tags])))

(defn panel-item [member]
  [:div.panel-block.is-block.panel-block-custom
   [:div.columns
    [:div.column.is-half
     [:a
      {:href (:link-to-profile member)}
      (:name member)]]
    [:div.column (group-tags (:groups member))]]])

(defn profile-popup
  [member]
  [:div.is-flex.is-centered
   [:a {:target "_blank" :href (:link-to-profile member)}
    [:h1.title.is-4 (:name member)]
    [:img.image.is-96x96 {:style {:margin "auto"} :src (:avatar member)}]]])

(defn members-map
  [model]
  [:div
   [:div
    {:id "map"
     :style {:height "400px"}
     :data-signals-locations (js-literal
                               (map (fn [member]
                                      [(get-in member [:location :latitude])
                                       (get-in member [:location :longitude])
                                       (render-html (profile-popup member))])
                                    (:members model)))
     :data-on-load__delay.500ms "galtAddMarkers($locations)"
     }]])

(defn search-results
  [model]
  [:div {:id "search-results"}
   (case (:active-tab model)
     :nearby (members-map model)
     (map panel-item (:members model)))])

(defn panel-tab
  [tab]
  [:a {:class (when (:active? tab) "is-active") :href (:href tab)} (:name tab)])

(defn present
  [model]
  [:div
   [:nav.panel
   [:p.panel-heading "Find members"]
   [:div.panel-block
    [:div.field.has-addons
     [:p {:class [:control :has-icons-left]}
      [:input.input.is-fullwidth {:placeholder "Search"
                                  :name "query"
                                  :data-bind "query"
                                  :data-on-keyup "evt.keyCode === 13 && @get('/members')"
                                  ; :data-on-keyup__debounce.500ms "@get('/members')"
                                  }]
      [:span {:class "icon is-left"}
       [:i {:class "fas fa-search" :aria-hidden "true"}]]]
     [:p.control [:button.button.is-primary {:data-on-click (d*-backend-action "/members")} "Search"]]]]
   [:p.panel-tabs (map panel-tab (:tabs model))]
   (search-results model)]])

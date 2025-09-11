(ns galt.members.adapters.presentation.members-list
  (:require
   [galt.core.views.datastar-helpers :refer [d*-backend-action]]))

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
     [:a {:href (:link-to-profile member)} (:name member)]]
    [:div.column (group-tags (:groups member))]]])

(defn search-results
  [results]
  [:div {:id "search-results"} results])

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
                    ; :data-on-keyup "evt.keyCode === 13 && @get('/members/search')"
                    :data-on-keyup__debounce.500ms "@get('/members/search')"
                    }]
     [:span {:class "icon is-left"}
      [:i {:class "fas fa-search" :aria-hidden "true"}]]
     ]
     [:p.control [:button.button.is-primary {:data-on-click (d*-backend-action "/members/search")} "Search"]]
    ]]
   [:p.panel-tabs
    [:a {:class "is-active"} "All"]
    [:a "Most active"]
    [:a "Most recent"]
    [:a "Near you"]]
   (search-results (map panel-item model))
   ; (search-results (list))
   [:div.panel-block
    [:button {:class "button is-link is-outlined is-fullwidth"}
     "Reset all filters"]]]])

(ns galt.events.adapters.presentation.new-event)

(defn present
  [model]
  [:div
   [:form {:method "POST" :action "/events"}
    [:input {:type "hidden" :name "group-id" :value (:group-id model)}]
    [:input {:type "hidden" :name "location-id" :value (:location-id model)}]
    [:div.field
     [:label.label "Name"]
     [:div.control
      [:input.input {:name "name" :value (:title model)}]]]
    [:div.field
     [:label.label "Start time"]
     [:div.control
      [:input.input {:name "start-time" :type "datetime-local"}]]]
    [:div.field
     [:label.label "End time"]
     [:div.control
      [:input.input {:name "end-time" :type "datetime-local"}]]]
    [:div.field
     [:label.label "Type"]
     [:div.control
      [:div.radios
       [:label.radio
        [:input {:name "type" :type "radio" :value "live"}]
        "Live"]
       [:label.radio
        [:input {:name "type" :type "radio" :value "online"}]
        "Online"]]
      ]]
    [:div.field
     [:label.label "Publish at"]
     [:div.control
      ; TODO default to now, disable past dates
      [:input.input {:name "publish-at" :type "datetime-local"}]]]
    [:div.field
     [:label.label "Description"]
     [:div.control
      [:textarea.textarea {:name "description"} (:description model)]]
     [:p.help "Can use Markdown"]]
    [:div.field
     [:div.control
      [:button.button.is-primary "Save"]]]]])

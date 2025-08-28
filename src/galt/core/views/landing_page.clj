(ns galt.core.views.landing-page
  (:require
    [clojure.pprint]))

(defn page
  [model]
  [:div
   [:h1 "Who is John Galt?"]
   [:div#system-status "You came from"]
   [:pre
    [:code (with-out-str (clojure.pprint/pprint model))]]
   [:input#greeting-in {:type :text :data-bind "greeting"}]
   [:div#greeting-out {:data-text "$greeting" :style {:border-color :red}}]
   [:button.button {:data-on-click "@get('/answers')"} "Speak with the server"]])

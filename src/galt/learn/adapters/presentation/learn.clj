(ns galt.learn.adapters.presentation.learn
  (:require [galt.learn.adapters.curriculum :as data]))

(defn curriculum-list
  [curriculum]
  (into [:div]
        (for [{:keys [slug title lessons]} (:modules curriculum)]
          [:section
           [:h2 title]
           (into [:ul]
                 (for [lesson lessons
                       :when (not= :locked (:status lesson))]
                   [:li [:a {:href (data/lesson-href slug (:slug lesson))} (:title lesson)]]))])))

(defn present
  [curriculum]
  {:content (curriculum-list curriculum)})

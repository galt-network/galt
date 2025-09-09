(ns galt.members.adapters.presentation.profile
  (:require
    [galt.members.adapters.presentation.non-member-profile :as non-member]
    [galt.members.adapters.presentation.member-profile :as member]))

(defn present
  [model]
  [:div
   [:h1.title.is-2 (str "Your profile, " (:name model))]
   (if (:member? model)
     (member/present model)
     (non-member/present model))])

(defn present-error
  [errors]
  [:article.message.is-danger
     [:div.message-header [:p "Errors in creating group"]]
     [:div.message-body [:ul (map (fn [e] [:li e]) errors)]]])

(ns galt.members.adapters.presentation.profile
  (:require
    [galt.members.adapters.presentation.non-member-profile :as non-member]))

(defn present
  [model]
  [:div
   [:h1.title.is-2 (str "Your profile, " (get-in model [:user :users/name]))]
   (if (:member? model)
     [:p "You are a member"]
     (non-member/present model))])

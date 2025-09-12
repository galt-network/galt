(ns galt.invitations.adapters.presentation.invitation)

(defn present
  [model]
  [:div
   [:h1.title.is-1 "You are invited to join GALT - Global Alliance for Libertarian Transformation"]
   [:div.content
    (get-in model [:invitation :content])]])

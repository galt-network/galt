(ns galt.members.adapters.presentation.non-member-profile)

(defn- introduction
  []
  [:div.content
   [:p "You are currently a user of this site. This allows you to view
       the public content that members of groups have published and search amongst the
       groups and existing members, people who might interest you."]
   [:p "To use the full functionality of GALT, you should become a member, for which you'll need
       an invitation from an existing member. Sometimes they do it at their events or you can
       ask somebody you know who is a member, to send you one."]])

(defn present
  [model]
  [:div
   (introduction)
   [:a.button.is-primary {:href (:new-invitation-url model)} "Request an invitation"]])

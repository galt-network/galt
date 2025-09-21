(ns galt.members.adapters.presentation.non-member-profile)

(defn- introduction
  []
  [:div.content
   [:p "You are currently a user of this site. This allows you to view
       the public content that members of groups have published and search amongst the
       groups and existing members, people who might interest you."]
   [:p "To use the full functionality of GALT, you should become a member,
       which has many advantages, allowing you to:"]
   [:ul
    [:li "Create and join groups"]
    [:li "Find and make friends with other libertarians around the world"]
    [:li "Post to groups and exchange messages with other members"]
    [:li "Create initiatives and events"]
    [:li "Contribute to the spreading of libertarian ideas for a better future"]]
   [:p "If that interests you, become a member by clicking the button below."]])

(defn present
  [link-url]
  [:div
   (introduction)
   [:a.button.is-primary {:href link-url} "I want to become a member"]])

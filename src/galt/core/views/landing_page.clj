(ns galt.core.views.landing-page
  (:require
    [clojure.pprint]))

(def version-1
  [:div
     ;; Hero Section
    [:section.hero.is-primary.is-bold
     [:div.hero-body
      [:div.container.has-text-centered
       [:h1.title.is-1 "Connect, Collaborate, and Liberate"]
       [:h2.subtitle.is-3 "Build Freer Communities with GALT"]
       [:p "A decentralized platform for libertarians to share ideas, form groups, organize events, and forge alliances against state overreach."]
       [:a.button.is-large.is-light {:href "/signup"} "Get Started"]]]]

    ;; About Section
    [:section.section {:id "about"}
     [:div.container
      [:div.columns
       [:div.column.is-two-thirds
        [:h2.title.is-2 "About GALT"]
        [:p "GALT empowers individuals with libertarian and anarcho-capitalist ideals to connect, produce analysis, and propose solutions for reducing state coercion and restoring individual freedoms in local communities."]]
       [:div.column.is-one-third
        [:figure.image [:img {:src "https://example.com/freedom-network.jpg" :alt "Decentralized network illustration"}]]]]]]

    ;; Features Section
    [:section.section.is-medium {:id "features"}
     [:div.container
      [:h2.title.is-2.has-text-centered "Key Features"]
      [:div.columns
       [:div.column
        [:div.card
         [:div.card-content
          [:div.media
           [:div.media-left [:figure.image.is-48x48 [:img {:src "https://example.com/user-icon.png" :alt "Members"}]]]
           [:div.media-content [:p.title.is-4 "Members & Profiles"]]]
          [:div.content "Create profiles with name, location, and description to find and connect with like-minded libertarians."]]]]
       [:div.column
        [:div.card
         [:div.card-content
          [:div.media
           [:div.media-left [:figure.image.is-48x48 [:img {:src "https://example.com/group-icon.png" :alt "Groups"}]]]
           [:div.media-content [:p.title.is-4 "Groups"]]]
          [:div.content "Join or create groups to collaborate on ideas for freer societies."]]]]
       [:div.column
        [:div.card
         [:div.card-content
          [:div.media
           [:div.media-left [:figure.image.is-48x48 [:img {:src "https://example.com/event-icon.png" :alt "Events"}]]]
           [:div.media-content [:p.title.is-4 "Events"]]]
          [:div.content "Organize events with locations and dates to propagate libertarian principles."]]]]
       [:div.column
        [:div.card
         [:div.card-content
          [:div.media
           [:div.media-left [:figure.image.is-48x48 [:img {:src "https://example.com/post-icon.png" :alt "Posts"}]]]
           [:div.media-content [:p.title.is-4 "Posts & Comments"]]]
          [:div.content "Share articles and engage in discussions on topics like voluntaryism and individual rights."]]]]]]]

    ;; Benefits Section
    [:section.section
     [:div.container
      [:h2.title.is-2.has-text-centered "Why Join GALT?"]
      [:div.columns
       [:div.column [:p "Escape isolation and build resilient networks free from state interference."]]
       [:div.column [:p "Collaborate on proposals to reclaim personal sovereignty in your community."]]]
      [:div.has-text-centered [:a.button.is-primary.is-large {:href "/signup"} "Join the Alliance"]]]]

    ;; Footer
    [:footer.footer
     [:div.container
      [:div.columns
       [:div.column [:p "© 2025 GALT – Empowering Individual Liberty"]]
       [:div.column.has-text-right
        [:a {:href "/privacy"} "Privacy"] " | "
        [:a {:href "/terms"} "Terms"] " | "
        [:a {:href "mailto:contact@galt.is"} "Contact"]]]]]]
)

(def version-2
  [:div
   ;; Hero Section (unchanged)
   [:section.hero.is-primary.is-bold
    [:div.hero-body
     [:div.container.has-text-centered
      [:h1.title.is-1 "Connect, Collaborate, and Liberate"]
      [:h2.subtitle.is-3 "Build Freer Communities with GALT"]
      [:p "A decentralized platform for libertarians to share ideas, form groups, organize events, and forge alliances against state overreach."]
      [:a.button.is-large.is-light {:href "/signup"} "Get Started"]]]]

   ;; About Section (unchanged)
   [:section.section {:id "about"}
    [:div.container
     [:div.columns
      [:div.column.is-two-thirds
       [:h2.title.is-2 "About GALT"]
       [:p "GALT empowers individuals with libertarian and anarcho-capitalist ideals to connect, produce analysis, and propose solutions for reducing state coercion and restoring individual freedoms in local communities."]]
      [:div.column.is-one-third
       [:figure.image [:img {:src "https://example.com/freedom-network.jpg" :alt "Decentralized network illustration"}]]]]]]

   ;; Features Section (unchanged)
   [:section.section.is-medium {:id "features"}
    [:div.container
     [:h2.title.is-2.has-text-centered "Key Features"]
     [:div.columns
      [:div.column
       [:div.card
        [:div.card-content
         [:div.media
          [:div.media-left [:figure.image.is-48x48 [:img {:src "https://example.com/user-icon.png" :alt "Members"}]]]
          [:div.media-content [:p.title.is-4 "Members & Profiles"]]]
         [:div.content "Create profiles with name, location, and description to find and connect with like-minded libertarians."]]]]
      [:div.column
       [:div.card
        [:div.card-content
         [:div.media
          [:div.media-left [:figure.image.is-48x48 [:img {:src "https://example.com/group-icon.png" :alt "Groups"}]]]
          [:div.media-content [:p.title.is-4 "Groups"]]]
         [:div.content "Join or create groups to collaborate on ideas for freer societies."]]]]
      [:div.column
       [:div.card
        [:div.card-content
         [:div.media
          [:div.media-left [:figure.image.is-48x48 [:img {:src "https://example.com/event-icon.png" :alt "Events"}]]]
          [:div.media-content [:p.title.is-4 "Events"]]]
         [:div.content "Organize events with locations and dates to propagate libertarian principles."]]]]
      [:div.column
       [:div.card
        [:div.card-content
         [:div.media
          [:div.media-left [:figure.image.is-48x48 [:img {:src "https://example.com/post-icon.png" :alt "Posts"}]]]
          [:div.media-content [:p.title.is-4 "Posts & Comments"]]]
         [:div.content "Share articles and engage in discussions on topics like voluntaryism and individual rights."]]]]]]]

   ;; New: Community Stats Section
   [:section.section
    [:div.container
     [:h2.title.is-2.has-text-centered "Our Growing Alliance"]
     [:div.tile.is-ancestor
      [:div.tile.is-parent
       [:article.tile.is-child.notification.is-info
        [:p.title "Members United"]
        [:p.subtitle "42"]  ;; Fetch via API: e.g., (http/get "/api/members/count")
        [:div.content "Pioneers fighting for individual rights."]]]
      [:div.tile.is-parent
       [:article.tile.is-child.notification.is-warning
        [:p.title "Active Groups"]
        [:p.subtitle "15"]  ;; Fetch via API: e.g., (http/get "/api/groups/count")
        [:div.content "Communities building freer societies."]]]
      [:div.tile.is-parent
       [:article.tile.is-child.notification.is-success
        [:p.title "Upcoming Events"]
        [:p.subtitle "8"]  ;; Fetch via API: e.g., (http/get "/api/events/upcoming/count")
        [:div.content "Gatherings to propagate libertarian ideas."]]]]]]

   ;; New: Recent Activity Section
   [:section.section.is-medium
    [:div.container
     [:h2.title.is-2.has-text-centered "Recent Activity"]
     [:p.subtitle.has-text-centered "See what's happening in the fight for freedom"]
     [:div.columns
      ;; Example: Fetch latest 3 posts/events via API: (http/get "/api/recent-activity?limit=3")
      [:div.column
       [:div.card
        [:div.card-header [:p.card-header-title "Latest Post: Voluntaryism in Action"]]
        [:div.card-content [:p "Discussion on reducing state coercion through local initiatives. Posted by Alice in Austin."]]
        [:div.card-footer [:a.card-footer-item "Read More"]]]]
      [:div.column
       [:div.card
        [:div.card-header [:p.card-header-title "New Event: Liberty Meetup"]]
        [:div.card-content [:p "Join us in Berlin on Oct 15 for talks on anarcho-capitalism. Organized by Berlin Freedom Group."]]
        [:div.card-footer [:a.card-footer-item "RSVP"]]]]
      [:div.column
       [:div.card
        [:div.card-header [:p.card-header-title "New Group: Texas Libertarians"]]
        [:div.card-content [:p "Focused on property rights and free markets. 5 members strong."]]
        [:div.card-footer [:a.card-footer-item "Join"]]]]]]]

   ;; Benefits Section (unchanged, but moved after new sections)
   [:section.section
    [:div.container
     [:h2.title.is-2.has-text-centered "Why Join GALT?"]
     [:div.columns
      [:div.column [:p "Escape isolation and build resilient networks free from state interference."]]
      [:div.column [:p "Collaborate on proposals to reclaim personal sovereignty in your community."]]]
     [:div.has-text-centered [:a.button.is-primary.is-large {:href "/signup"} "Join the Alliance"]]]]

   ;; Footer (unchanged)
   [:footer.footer
    [:div.container
     [:div.columns
      [:div.column [:p "© 2025 GALT – Empowering Individual Liberty"]]
      [:div.column.has-text-right
       [:a {:href "/privacy"} "Privacy"] " | "
       [:a {:href "/terms"} "Terms"] " | "
       [:a {:href "mailto:contact@galt.is"} "Contact"]]]]]])

(defn page [model] version-2)

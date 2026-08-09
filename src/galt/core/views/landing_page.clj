(ns galt.core.views.landing-page
  (:require
   [galt.shared.presentation.translations :refer [i18n]]))

(defn- hero-banner [{:keys [authenticated? groups-href map-href login-href]}]
  [:section.hero.is-primary.is-bold
   [:div.hero-body
    [:div.container.has-text-centered
     [:h1.title.is-1 "Find your people. Build freer communities."]
     [:h2.subtitle.is-3 "GALT connects freedom-minded people — anonymously, through groups, events and direct communication."]
     [:p "No state approval. No data harvesting. Payments over Bitcoin Lightning, self-moderation by each group."]
     [:div.buttons.is-centered
      (if authenticated?
        [:a.button.is-large.is-light {:href groups-href} "Go to your groups"]
        [:a.button.is-large.is-light {:href map-href} "Explore the map"])
      (when-not authenticated?
        [:a.button.is-large.is-outlined {:href login-href} "Log in with Lightning"])]]]])

(defn- stats-section [{:keys [stats]}]
  [:section.section
   [:div.container
    [:div.columns.has-text-centered
     [:div.column
      [:p.title.is-2 (:members stats)]
      [:p.subtitle "members"]]
     [:div.column
      [:p.title.is-2 (:groups stats)]
      [:p.subtitle "groups"]]
     [:div.column
      [:p.title.is-2 (:events stats)]
      [:p.subtitle "events"]]]]])

(defn- recent-activity-section [{:keys [recent-groups recent-posts recent-events groups-href posts-href events-href]}]
  [:section.section
   [:div.container
    [:h2.title.is-3 "What's happening on GALT"]
    (when (seq recent-groups)
      [:div.columns
       [:div.column.is-half
        [:h3.subtitle.is-5 "Newest groups"]
        (for [g recent-groups]
          [:div.card.mb-3
           [:div.card-content
            [:a {:href (:href g)}
             [:p.title.is-5 (:name g)]]]])]])
    (when (seq recent-posts)
      [:div.columns
       [:div.column.is-half
        [:h3.subtitle.is-5 "Newest posts"]
        (for [p recent-posts]
          [:div.card.mb-3
           [:div.card-content
            [:a {:href (:href p)}
             [:p.title.is-5 (:title p)]]
            (when-let [author (:author p)]
              [:p.subtitle.is-7 "by " author])]])]])
    (when (seq recent-events)
      [:div.columns
       [:div.column.is-half
        [:h3.subtitle.is-5 "Newest events"]
        (for [e recent-events]
          [:div.card.mb-3
           [:div.card-content
            [:a {:href (:href e)}
             [:p.title.is-5 (:name e)]]]])]])
    (when (and (seq recent-groups) groups-href)
      [:p [:a {:href groups-href} "Browse all groups →"]])
    (when (seq recent-posts)
      [:p [:a {:href posts-href} "Browse all posts →"]])
    (when (seq recent-events)
      [:p [:a {:href events-href} "Browse all events →"]])]])

(defn about-section
  [model]
  [:section.section {:id "about"}
   [:div.container
    [:div.columns
     [:div.column.is-two-thirds
      [:h2.title.is-2 (i18n :landing/about-title)]
      [:p (i18n :landing/about-content)]]
     [:div.column.is-one-third
      [:figure.image
       [:img {:src "/assets/images/about-galt-illustration.jpg" :alt "Decentralized network illustration"}]]]]]])

(defn features-section
  [{:keys [groups-href members-href events-href]}]
  (let [features [[:members members-href]
                  [:groups groups-href]
                  [:events events-href]
                  [:improve "https://github.com/galt-network/galt"]]]
    [:section.section.is-medium {:id "features"}
     [:div.container
      [:h2.title.is-2.has-text-centered "Key Features"]
      [:div.columns.is-multiline
       (for [[feature href] features]
         (let [{:keys [title content]} (i18n (keyword "landing.features" (name feature)))]
           [:div.column.is-half
            [:div.card
             [:div.card-content
              [:div.media
               [:div.media-content [:p.title.is-4 title]]]
              [:div.content content]
              [:a.button.is-link {:href href} "Learn more"]]]]))]]]))

(defn trust-section
  []
  [:section.section
   [:div.container
    [:h2.title.is-2.has-text-centered "Built on trust, not on trust-us"]
    [:div.columns.is-multiline
     [:div.column.is-half
      [:div.card
       [:div.card-content
        [:p.title.is-5 "Optional anonymity"]
        [:p "Log in with your Lightning wallet — no email, no phone number, no tracking."]]]]
     [:div.column.is-half
      [:div.card
       [:div.card-content
        [:p.title.is-5 "Group self-moderation"]
        [:p "Every group sets its own rules. There is no central authority that can silence you."]]]]
     [:div.column.is-half
      [:div.card
       [:div.card-content
        [:p.title.is-5 "Bitcoin Lightning payments"]
        [:p "Support groups with sats. GALT never holds your funds — payments go directly from your wallet."]]]]
     [:div.column.is-half
      [:div.card
       [:div.card-content
        [:p.title.is-5 "Open source"]
        [:p "The code is public. Anyone can inspect it, run their own instance, or fork the project."]]]]]]])

(defn- my-groups-section [{:keys [my-groups groups-href]}]
  (when (seq my-groups)
    [:section.section
     [:div.container
      [:h2.title.is-3 "Your groups"]
      [:div.columns.is-multiline
       (for [g my-groups]
         [:div.column.is-one-third
          [:div.card
           [:div.card-content
            [:a {:href (:href g)}
             [:p.title.is-5 (:name g)]]]]])
      [:p [:a {:href groups-href} "Browse all groups →"]]]]]))

(defn page [model]
  [:div
   (hero-banner model)
   (stats-section model)
   (recent-activity-section model)
   (about-section model)
   (my-groups-section model)
   (features-section model)
   (trust-section)])

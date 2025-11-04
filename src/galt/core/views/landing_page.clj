(ns galt.core.views.landing-page
  (:require
   [galt.core.infrastructure.version :as version]
   [galt.shared.presentation.translations :refer [i18n]]))

(defn- hero-banner [{:keys [new-user?]}]
  [:section.hero.is-primary.is-bold
     [:div.hero-body
      [:div.container.has-text-centered
       [:h1.title.is-1 "Connect, Collaborate and Convince"]
       [:h2.subtitle.is-3 "Build Freer Communities with GALT"]
       [:p "A decentralized platform for libertarians to share ideas, form groups, organize events, and forge alliances for a more civilized, peaceful and prosperous society"]
       (when new-user? [:a.button.is-large.is-light {:href "/signup"} "Get Started"])]]])

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
  [model]
  (let [features [:members :groups :events :improve]
        i18n-base :landing.features
        features-content (map (fn [feature]
                                (i18n (keyword (name i18n-base) (name feature))))
                              features)]
    [:section.section.is-medium {:id "features"}
    [:div.container
     [:h2.title.is-2.has-text-centered "Key Features"]
     [:div.columns.is-multiline
      (for [feature features-content]
        [:div.column.is-half
         [:div.card
          [:div.card-content
           [:div.media
            [:div.media-content [:p.title.is-4 (:title feature)]]]
           [:div.content (:content feature)]]]])]]]))

(defn page [{:keys [new-user?] :as model}]
  [:div
    (hero-banner (select-keys model [:new-user?]))
    (about-section model)
    (features-section model)
    #_[:footer.footer
     [:div.container
      [:div.columns
       [:div.column [:p "© 2025 GALT – Empowering Individual Liberty"]]
       [:div.column.has-text-right
        [:p [:a {:href "https://github.com/galt-network/galt" :target "_blank"}
          [:span.icon
           [:i.fa.fa-brands.fa-github]]
          "Source code"
          ]]
        [:p [:a {:href (str "https://github.com/galt-network/galt/tree/" (version/commit-hash))
                 :target "_blank"}
          [:span.icon
           [:i.fa.fa-solid.fa-code-branch]]
          "Version: " (version/commit-hash) " " (System/getenv "GALT_ENV")]]]]]]])

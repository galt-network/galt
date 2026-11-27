(ns galt.core.views.new-layout
  (:require [clojure.string :as str]
            [galt.core.infrastructure.version :as version]
            [hiccup.util :as hu]))

(def ^:private nav-items
  [{:title "Connect" :href "/connect" :icon "hub" :tab-icon "hub"}
   {:title "Learn" :href "/learn" :icon "school" :tab-icon "auto_stories"}
   {:title "Events" :href "/events" :icon "calendar_month" :tab-icon "calendar_month"}])

;; inactive and active anchors of the header nav, as in lesson.html
(def ^:private nav-link-classes
  ["relative" "flex" "items-center" "gap-2" "py-2" "text-sm" "font-medium" "tracking-wide"
   "text-text-muted" "hover:text-primary" "hover:drop-shadow-[0_0_8px_rgba(245,194,66,0.6)]"
   "transition-all" "duration-300" "group"])

(def ^:private nav-link-active-classes
  ["relative" "flex" "items-center" "gap-2" "py-2" "text-sm" "font-semibold" "tracking-wide"
   "text-primary" "transition-all" "duration-200" "after:content-['']" "after:absolute"
   "after:bottom-0" "after:left-0" "after:right-0" "after:h-[2px]"
   "after:bg-primary-container" "after:rounded-full" "after:shadow-[0_0_10px_#f5c242]"])

(def ^:private tab-classes
  ["flex" "flex-col" "items-center" "gap-1" "min-h-11" "text-text-muted"
   "hover:text-text-primary" "transition-colors"])

(defn icon
  [classes name]
  [:span {:class (conj classes "material-symbols-outlined")} name])

(defn content
  [hiccup]
  [:main {:id "content"
          :class ["flex-1" "w-full" "pt-16" "md:pt-20" "pb-28" "md:pb-8" "bg-canvas-base"]}
   [:div {:class ["max-w-7xl" "mx-auto" "px-4" "sm:px-6" "lg:px-8" "w-full"]}
    hiccup]])

(defn- current?
  [model item]
  (str/starts-with? (or (:path model) "") (:href item)))

(defn- brand
  []
  [:a {:class ["flex" "items-center" "gap-3" "group"] :href "/"}
   [:span {:class ["flex" "items-center" "justify-center" "w-8" "h-8" "rounded"
                   "bg-surface-container-high" "border" "border-border-gold" "text-primary"
                   "font-headline-md" "text-headline-sm" "transition-transform" "duration-300"
                   "group-hover:scale-105" "shadow-[0_0_8px_rgba(245,194,66,0.2)]"]}
    "G"]
   [:div {:class ["flex" "flex-col" "leading-none"]}
    [:span {:class ["font-headline-sm" "text-headline-sm" "font-semibold" "tracking-wider" "text-primary"]}
     "GALT"]
    [:span {:class ["font-label-sm" "text-[9px]" "tracking-widest" "uppercase" "text-secondary"
                    "font-medium" "-mt-0.5"]}
     "ACADEMY"]]])

(defn- nav-link
  [model {title :title href :href icon-name :icon :as item}]
  (let [active? (current? model item)]
    [:a (cond-> {:href href
                 :class (if active? nav-link-active-classes nav-link-classes)}
          active? (assoc :aria-current "page"))
     (icon ["text-[18px]" (if active? "text-primary" "text-text-muted group-hover:text-primary")]
           icon-name)
     [:span title]
     (when-not active?
       [:span {:class ["pointer-events-none" "absolute" "bottom-0" "left-1/2" "-translate-x-1/2"
                       "h-[2px]" "w-0" "rounded-full" "bg-primary-container/80"
                       "shadow-[0_0_8px_rgba(245,194,66,0.5)]" "transition-all" "duration-300"
                       "group-hover:w-full"]}])]))

(defn- theme-toggle
  []
  [:button {:type "button"
            :data-theme-toggle true
            :aria-pressed "false"
            :aria-label "Switch to light theme"
            :title "Switch theme"
            :class ["flex" "items-center" "gap-1.5" "px-3" "py-1.5" "shrink-0" "cursor-pointer"
                    "rounded-full" "bg-surface-container" "border" "border-border-gold/40"
                    "text-primary" "font-label-sm" "text-[11px]" "uppercase" "tracking-widest"
                    "font-semibold" "shadow-[0_0_10px_rgba(245,194,66,0.1)]"
                    "hover:border-border-gold/60" "hover:bg-surface-container-high"
                    "transition-all" "duration-300"]}
   (icon ["text-[16px]"] "dark_mode")
   [:span {:data-theme-label true :class ["hidden" "sm:inline"]} "Dark"]])

;; the search field and rank pill of the reference are left out
(defn- header
  [model]
  [:header {:class ["fixed" "top-0" "left-0" "w-full" "z-50" "bg-canvas-base/90"
                    "backdrop-blur-xl" "border-b" "border-border-subtle"]}
   [:div {:class ["h-16" "md:h-20" "max-w-7xl" "mx-auto" "px-4" "sm:px-6" "lg:px-8" "flex"
                  "items-center" "justify-between" "gap-4"]}
    [:div {:class ["flex" "items-center" "gap-6" "lg:gap-8"]}
     (brand)
     (into [:nav {:class ["hidden" "md:flex" "items-center" "gap-6" "lg:gap-7"]}]
           (map (partial nav-link model) nav-items))]
    [:div {:class ["flex" "items-center" "gap-3" "shrink-0"]}
     (theme-toggle)
     [:a {:class ["group" "flex" "items-center" "gap-2" "shrink-0"]
          :href (-> model :user :href)
          :title (-> model :user :name)}
      [:span {:class ["w-9" "h-9" "rounded-full" "shrink-0" "overflow-hidden"
                      "bg-surface-container" "border" "border-border-gold/30"
                      "flex" "items-center" "justify-center"
                      "shadow-[0_0_10px_rgba(245,194,66,0.15)]"
                      "group-hover:border-primary/60" "group-hover:scale-105"
                      "transition-all"]}
       (if (-> model :user :placeholder?)
         (icon ["text-[18px]" "text-primary"] "person")
         [:img {:class ["w-full" "h-full" "object-cover"]
                :src (-> model :user :avatar)
                :alt ""}])]
      [:span {:class ["hidden" "sm:inline" "font-label-sm" "text-label-sm" "tracking-wide"
                      "text-text-primary"]}
       (-> model :user :name)]]]]])

(defn- tab
  [model {:keys [title href tab-icon] :as item}]
  (let [active? (current? model item)]
    [:a (cond-> {:href href
                 :class (if active?
                          (conj tab-classes "relative" "text-primary"
                                "drop-shadow-[0_0_8px_rgba(245,194,66,0.35)]")
                          tab-classes)}
          active? (assoc :aria-current "page"))
     (icon ["text-[22px]"] tab-icon)
     [:span {:class ["font-label-sm" "text-label-sm" "uppercase" "tracking-wide"]} title]
     (when active?
       [:span {:class ["pointer-events-none" "absolute" "bottom-1" "w-5" "h-0.5"
                       "bg-primary-container" "rounded-full"
                       "shadow-[0_0_10px_#f5c242,0_0_16px_rgba(245,194,66,0.45)]"]}])]))

(defn- tab-bar
  [model]
  (into
    [:nav {:class ["md:hidden" "fixed" "bottom-0" "left-0" "right-0" "z-50"
                   "h-16" "bg-canvas-base/95" "backdrop-blur-xl" "border-t" "border-border-subtle"
                   "px-3" "pb-safe" "flex" "items-center" "justify-around"]}]
    (concat
      (map (partial tab model) nav-items)
      [(tab model {:title "Profile" :href (-> model :user :href) :tab-icon "person"})])))

(defn- footer
  []
  [:footer {:class ["hidden" "md:block" "w-full" "bg-surface-container-lowest" "border-t"
                    "border-border-subtle" "mt-12" "py-8"]}
   [:div {:class ["max-w-7xl" "mx-auto" "px-4" "sm:px-6" "lg:px-8" "flex" "flex-col"
                  "sm:flex-row" "items-center" "justify-between" "gap-4"
                  "text-on-surface-variant" "font-label-md" "text-label-md"]}
    [:div "© 2025 Global Alliance for Libertarian Transformation. All Sovereignty Preserved."]
    [:div {:class ["flex" "items-center" "gap-6" "font-label-sm" "text-label-sm"]}
     [:a {:href "https://github.com/galt-network/galt" :target "_blank"
          :class ["hover:text-text-primary"]}
      "Source code"]
     [:a {:href (str "https://github.com/galt-network/galt/tree/" (version/commit-hash))
          :target "_blank" :class ["hover:text-text-primary"]}
      "Version: " (version/commit-hash) " " (System/getenv "GALT_ENV")]]]])

;; `hidden` keeps the toast out of sight until Datastar runs; data-class toggles it
(defn- toast
  []
  [:div {:id "notification-container"
         :class ["hidden" "fixed" "top-4" "left-1/2" "z-[60]" "-translate-x-1/2" "max-w-sm"
                 "rounded-xl" "border" "border-border-subtle" "bg-surface-container-high"
                 "px-4" "py-3" "shadow-lg"]
         :data-class:hidden "!$notification-visible"
         :data-class:border-status-error "$notification-is-danger"}
   [:button {:type "button" :aria-label "Dismiss"
             :class ["absolute" "top-1" "right-2" "text-text-muted"]
             :data-on:click "$notification-visible = false"}
    (icon ["text-[18px]"] "close")]
   [:p {:class ["pr-6" "text-body-sm" "text-on-surface"] :data-text "$notification-text"}]])

(defn main-layout
  [model]
  [:html {:class "dark" :lang "en"}
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    ;; apply the saved theme before first paint so a light reload never flashes dark
    [:script (hu/raw-string "(function(){try{if(localStorage.getItem('galt-theme')==='light'){var r=document.documentElement;r.classList.remove('dark');r.classList.add('light')}}catch(e){}})()")]
    [:title (str (:page-title model) " | Galt")]
    [:link {:rel "stylesheet"
            :href "https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;500;600&family=Playfair+Display:ital,wght@0,500;0,600;0,700;1,500&family=Plus+Jakarta+Sans:wght@400;500;600;700&display=swap"}]
    [:link {:rel "stylesheet"
            :href "https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200"}]
    [:link {:rel "stylesheet" :href "/assets/css/tailwind.css"}]
    [:script {:type "module"
              :src "https://cdn.jsdelivr.net/gh/starfederation/datastar@v1.0.2/bundles/datastar.js"}]]
   [:body {:class ["bg-canvas-base" "text-on-surface" "font-body-md" "text-body-md"
                   "min-h-screen" "flex" "flex-col" "selection:bg-primary-container"
                   "selection:text-canvas-base"]}
    (header model)
    (content (:content model))
    (toast)
    (tab-bar model)
    (footer)
    [:script (hu/raw-string "(function(){var b=document.querySelector('[data-theme-toggle]');if(!b)return;function s(){var l=document.documentElement.classList.contains('light');b.setAttribute('aria-pressed',String(l));b.setAttribute('aria-label',l?'Switch to dark theme':'Switch to light theme');b.querySelector('.material-symbols-outlined').textContent=l?'light_mode':'dark_mode';b.querySelector('[data-theme-label]').textContent=l?'Light':'Dark'}s();b.addEventListener('click',function(){var r=document.documentElement,l=r.classList.contains('light');r.classList.toggle('light',!l);r.classList.toggle('dark',l);try{localStorage.setItem('galt-theme',l?'dark':'light')}catch(e){}s()})})()")]]])

(ns galt.learn.adapters.presentation.lesson
  (:require [galt.core.views.new-layout :as layout]
            [galt.learn.adapters.presentation.articles.subjective-theory :as subjective-theory]))

(def ^:private articles
  {"value-theory" subjective-theory/body})

(defn not-found
  []
  {:page-title "Lesson not found"
   :content [:div {:class ["max-w-7xl" "mx-auto" "px-4" "py-16"]}
             [:h1 {:class ["font-headline-lg" "text-headline-lg" "text-text-primary"]}
              "Lesson not found"]
             [:a {:href "/learn" :class ["text-primary-container" "hover:underline"]}
              "Back to the curriculum"]]})

(defn- status-text
  [{:keys [status progress requires]}]
  (case status
    :completed "Completed"
    :in-progress (str "In Progress • " progress "%")
    :locked (str "Locked • Requires " requires)
    "Not Started"))

(defn- progress-value
  [{:keys [status progress]}]
  (case status
    :completed 100
    :in-progress progress
    0))

(defn- status-ribbon
  [{:keys [module number topic] :as lesson}]
  [:section {:class ["w-full" "flex" "flex-col" "gap-4" "pb-6"]}
   [:div {:class ["flex" "flex-wrap" "items-center" "justify-between" "gap-4"]}
    [:div {:class ["flex" "items-center" "gap-2" "font-label-md" "text-label-md" "text-on-surface-variant"]}
     [:a {:class ["hover:text-primary" "transition-colors" "flex" "items-center" "gap-1.5"]
          :href "/learn"}
      (layout/icon ["text-body-md"] "arrow_back")
      [:span "Curriculum"]]
     [:span {:class ["text-border-gold"]} "/"]
     [:span {:class ["text-on-surface-variant"]}
      (str "Module " (format "%02d" (:number module)) ": " (:track module))]
     [:span {:class ["text-border-gold"]} "/"]
     [:span {:class ["text-primary" "font-semibold"]}
      (str "Lesson " (format "%02d" number) ": " topic)]]
    [:div {:class ["flex" "flex-wrap" "items-center" "gap-2" "sm:gap-3"]}
     [:span {:class ["inline-flex" "items-center" "gap-1.5" "px-2.5" "py-1" "rounded" "bg-surface-container-high" "text-primary" "font-label-sm" "text-label-sm" "tracking-wider" "uppercase"]}
      [:span {:class ["w-1.5" "h-1.5" "rounded-full" "bg-secondary" "animate-pulse"]}]
      (status-text lesson)]
     (when-let [obj (:obj lesson)]
       [:span {:class ["inline-flex" "items-center" "gap-1.5" "px-2.5" "py-1" "rounded" "bg-surface-container-lowest" "text-secondary" "font-label-sm" "text-label-sm" "tracking-wide"]}
        (layout/icon ["text-body-sm" "text-secondary"] "token")
        (str "+" obj " OBJ AVAILABLE")])]]])

(defn- progress-dock
  [{:keys [section sections] :as lesson}]
  (let [percent (progress-value lesson)]
    [:div {:class ["p-3" "sm:p-4" "rounded-xl" "bg-surface-container-lowest" "shadow-md" "flex" "flex-col" "lg:flex-row" "lg:items-center" "justify-between" "gap-4"]}
     [:div {:class ["flex-1" "flex" "flex-col" "gap-2"]}
      [:div {:class ["flex" "items-center" "justify-between" "text-on-surface-variant" "font-label-sm" "text-label-sm"]}
       (when (and section sections)
         [:span {:class ["text-on-surface" "font-medium"]}
          (str "Section " section " of " sections)])
       [:span {:class ["text-primary" "font-semibold"]} (str percent "% COMPLETED")]]
      [:div {:class ["w-full" "h-1.5" "rounded-full" "bg-surface-container-high" "overflow-hidden"]}
       [:div {:class ["h-full" "bg-primary-container" "rounded-full" "transition-all" "duration-500" "shadow-sm"]
              :style (str "width: " percent "%;")}]]]]))

(defn- hero
  [{:keys [module topic summary title]}]
  [:section {:class ["w-full" "flex" "flex-col" "gap-6" "pt-2" "pb-10"]}
   [:div {:class ["flex" "flex-col" "gap-3" "max-w-4xl"]}
    [:div {:class ["inline-flex" "items-center" "gap-2" "text-secondary" "font-label-md" "text-label-md" "tracking-widest" "uppercase"]}
     [:span (str "Module " (format "%02d" (:number module)) " • " (:track module))]
     [:span "•"]
     [:span topic]]
    [:h1 {:class ["font-headline-lg" "sm:text-[42px]" "sm:leading-[50px]" "text-text-primary" "tracking-tight" "font-serif" "font-bold" "text-headline-lg"]}
     title]
    [:p {:class ["font-body-lg" "text-on-surface-variant" "leading-relaxed" "text-[17px]" "leading-[27px]" "font-normal"]}
     summary]]])

(defn- metadata-strip
  [{:keys [minutes questions obj]}]
  (let [entry (fn [icon-name text]
                [:div {:class ["flex" "items-center" "gap-1.5"]}
                 (layout/icon ["text-body-sm" "text-primary"] icon-name)
                 [:span text]])]
    [:div {:class ["flex" "flex-wrap" "items-center" "gap-4" "sm:gap-6" "py-3" "px-4" "rounded" "bg-surface-container" "font-label-sm" "text-label-sm" "text-on-surface-variant"]}
     (entry "schedule" (str "Estimated Read: " minutes " min"))
     (entry "quiz" (str questions " Questions"))
     (entry "token" (str "+" obj " OBJ"))]))

(defn- artwork
  [{:keys [module summary]}]
  [:figure {:class ["w-full" "flex" "flex-col" "gap-2" "rounded-lg" "border" "border-border-subtle" "overflow-hidden" "bg-surface-container-lowest" "shadow-xl"]}
   [:div {:class ["relative" "w-full" "h-[320px]" "sm:h-[440px]" "overflow-hidden" "bg-surface-container-high"]}
    [:img {:alt summary
           :class ["w-full" "h-full" "object-cover"]
           :src (str "/assets/images/learn/" (:image module) ".jpg")}]
    [:div {:class ["absolute" "inset-0" "bg-gradient-to-t" "from-surface-container-lowest" "via-transparent" "to-transparent" "opacity-90"]}]]])

(defn- reading-column
  [{:keys [slug]}]
  (into [:main {:class ["w-full" "flex" "flex-col" "gap-8"]}]
        (or (articles slug)
            [[:p {:class ["font-body-lg" "text-body-lg" "text-on-surface"]} "Lesson text coming soon."]])))

(defn- completion-ribbon
  [{:keys [next-lesson]}]
  [:section {:class ["w-full" "mt-6" "p-4" "sm:p-6" "rounded-xl" "bg-surface-container" "shadow-xl" "flex" "flex-col" "sm:flex-row" "items-center" "justify-between" "gap-4"]}
   [:div {:class ["flex" "items-center" "gap-3" "w-full" "sm:w-auto"]}
    [:a {:class ["font-body-sm" "text-body-sm" "text-on-surface-variant" "hover:text-primary" "transition-colors" "flex" "items-center" "gap-1"]
         :href "/learn"}
     (layout/icon ["text-body-md"] "west")
     [:span "Back to the curriculum"]]]
   (when next-lesson
     [:a {:class ["px-4" "py-2" "rounded" "bg-surface-container-high" "hover:bg-surface-bright" "text-primary" "font-headline-sm" "text-body-sm" "font-semibold" "flex" "items-center" "gap-2" "transition-colors"]
          :href (:href next-lesson)}
      [:span (str "Next: " (:title next-lesson))]
      (layout/icon ["text-body-sm"] "arrow_forward")])])

(defn present
  [{:keys [title] :as lesson}]
  {:page-title title
   :content [:div
             (status-ribbon lesson)
             (progress-dock lesson)
             (hero lesson)
             (metadata-strip lesson)
             (artwork lesson)
             (reading-column lesson)
             (completion-ribbon lesson)]})

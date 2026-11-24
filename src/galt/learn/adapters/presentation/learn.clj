(ns galt.learn.adapters.presentation.learn
  (:require [clojure.string :as str]
            [galt.core.views.new-layout :as layout]
            [galt.learn.adapters.curriculum :as data]))

(def ^:private member-stats
  {:rank "Sovereign Scholar" :obj "4,850 OBJ" :progress 68 :cleared "14 / 22"
   :streak "18" :evaluations "14/14 (100%)" :domains "6 Domains"})

(defn- hero
  []
  (let [{:keys [rank obj progress cleared streak evaluations]} member-stats
        [points unit] (str/split obj #" ")]
    [:section {:class ["relative" "overflow-hidden" "rounded-xl" "bg-surface-container-low" "shadow-[0_4px_24px_rgba(0,0,0,0.6)]" "p-5"]}
     [:div {:class ["absolute" "-top-12" "-right-12" "w-48" "h-48" "rounded-full" "bg-primary-container/10" "blur-3xl" "pointer-events-none"]}]
     [:div {:class ["absolute" "bottom-0" "left-1/4" "w-32" "h-16" "bg-secondary/5" "blur-2xl" "pointer-events-none"]}]
     [:div {:class ["relative" "z-10" "flex" "flex-col" "space-y-4"]}
      [:div {:class ["flex" "items-center" "justify-between" "gap-2"]}
       [:div {:class ["flex" "items-center" "gap-1.5"]}
        [:span {:class ["w-1.5" "h-1.5" "rounded-full" "bg-primary-container" "shadow-[0_0_8px_#F5C242]"]}]
        [:span {:class ["font-label-sm" "text-label-sm" "uppercase" "tracking-widest" "text-primary-container" "font-semibold"]}
         "Curriculum Ledger"]]
       [:div {:class ["inline-flex" "items-center" "gap-1" "px-2.5" "py-0.5" "rounded-full" "bg-surface-container" "shadow-[0_0_12px_rgba(245,194,66,0.15)]"]}
        (layout/icon ["text-[13px]" "text-primary"] "military_tech")
        [:span {:class ["font-label-sm" "text-label-sm" "uppercase" "text-primary" "font-semibold" "tracking-wider"]}
         (str "Lvl 4 " rank)]]]
      [:div {:class ["flex" "items-baseline" "justify-between" "pt-1"]}
       [:div
        [:div {:class ["flex" "items-baseline" "gap-2"]}
         [:span {:class ["font-display-lg-mobile" "text-display-lg-mobile" "text-text-primary" "tracking-tight" "font-semibold"]} points]
         [:span {:class ["font-label-md" "text-label-md" "text-primary" "tracking-wider" "uppercase" "font-semibold"]} (str unit " EARNED")]]
        [:p {:class ["font-body-sm" "text-body-sm" "text-text-muted" "mt-0.5"]}
         "Next Title: "
         [:span {:class ["text-text-primary" "font-medium"]} "Austrian Praxeologist"]
         " (6,000 OBJ)"]]
       [:div {:class ["hidden" "xs:flex" "flex-col" "items-end" "text-right"]}
        [:span {:class ["font-label-sm" "text-label-sm" "text-text-muted" "uppercase" "tracking-wider"]} "Curriculum Path"]
        [:span {:class ["font-label-md" "text-label-md" "text-secondary" "font-semibold"]} (str cleared " Cleared")]]]
      [:div {:class ["flex" "flex-col" "space-y-1.5"]}
       [:div {:class ["w-full" "h-2" "rounded-full" "bg-surface-container-highest" "overflow-hidden" "p-0.5"]}
        [:div {:class ["h-full" "rounded-full" "bg-primary-container" "shadow-[0_0_10px_rgba(245,194,66,0.5)]" "transition-all" "duration-700" "ease-out"]
               :style (str "width: " progress "%;")}]]
       [:div {:class ["flex" "items-center" "justify-between" "font-label-sm" "text-label-sm" "text-text-muted"]}
        [:span (str "Tier Progress: " progress "%")]
        [:span {:class ["text-on-surface-variant"]} "1,150 OBJ to Rank Up"]]]
      [:div {:class ["grid" "grid-cols-2" "gap-3" "pt-1"]}
       [:div {:class ["flex" "flex-col" "p-3" "rounded-lg" "bg-surface-container" "border" "border-border-gold/20"]}
        [:div {:class ["flex" "items-center" "gap-1.5" "text-primary"]}
         (layout/icon ["text-[16px]"] "local_fire_department")
         [:span {:class ["font-label-sm" "text-label-sm" "uppercase" "tracking-wider" "text-text-muted" "font-medium"]} "Streak"]]
        [:span {:class ["font-label-md" "text-label-md" "text-text-primary" "font-semibold" "mt-1.5"]} (str streak " Days")]]
       [:div {:class ["flex" "flex-col" "p-3" "rounded-lg" "bg-surface-container" "border" "border-border-gold/20"]}
        [:div {:class ["flex" "items-center" "gap-1.5" "text-primary-container"]}
         (layout/icon ["text-[16px]"] "verified")
         [:span {:class ["font-label-sm" "text-label-sm" "uppercase" "tracking-wider" "text-text-muted" "font-medium"]} "Evaluations"]]
        [:span {:class ["font-label-md" "text-label-md" "text-text-primary" "font-semibold" "mt-1.5"]} evaluations]]]]]))

(defn- pill
  [text active?]
  [:span {:class (into ["shrink-0" "flex" "items-center" "gap-1.5" "px-3" "py-1.5" "rounded-full" "transition-all"]
                       (if active?
                         ["bg-primary-container" "text-on-primary" "shadow-[0_0_12px_rgba(245,194,66,0.3)]"]
                         ["bg-surface-container" "text-on-surface" "border" "border-border-gold/40"]))}
   [:span {:class ["font-label-sm" "text-label-sm" "tracking-wider" "uppercase"]} text]])

(defn- pill-row
  [pills]
  [:div {:class ["flex" "items-center" "gap-2" "overflow-x-auto" "no-scrollbar" "-mx-margin-mobile" "px-margin-mobile" "py-1"]}
   (into [:<>] pills)])

(defn- domain-pills
  [curriculum]
  [:section {:class ["flex" "flex-col" "space-y-2"]}
   [:div {:class ["flex" "items-center" "justify-between"]}
    [:span {:class ["font-label-sm" "text-label-sm" "uppercase" "tracking-wider" "text-text-muted" "font-medium"]} "Knowledge Domains"]
    [:span {:class ["font-label-sm" "text-label-sm" "text-primary" "uppercase" "tracking-wider"]} (:domains member-stats)]]
   (pill-row
    (cons (pill (str "All Domains (" (count (mapcat :lessons (:modules curriculum))) ")") true)
          (for [{:keys [domain lessons]} (:modules curriculum)]
            (pill (str domain " (" (count lessons) ")") false))))])

(defn- status-count
  [curriculum status]
  (count (filter #(= status (:status %)) (mapcat :lessons (:modules curriculum)))))

(defn- status-filter
  [curriculum]
  [:section {:class ["flex" "flex-col" "space-y-2"]}
   [:div {:class ["flex" "items-center" "justify-between"]}
    [:span {:class ["font-label-sm" "text-label-sm" "uppercase" "tracking-wider" "text-text-muted" "font-medium"]} "Lesson Status"]
    [:span {:class ["font-label-sm" "text-label-sm" "text-text-muted" "uppercase" "tracking-wider"]} "Filter"]]
   (pill-row
    [(pill "All" true)
     (pill (str "New (" (status-count curriculum :available) ")") false)
     (pill (str "In Progress (" (status-count curriculum :in-progress) ")") false)
     (pill (str "Completed (" (status-count curriculum :completed) ")") false)])])

(defn- active-lesson
  [curriculum]
  (some (fn [module]
          (when-let [lesson (first (filter #(= :in-progress (:status %)) (:lessons module)))]
            (assoc lesson :module module)))
        (:modules curriculum)))

(defn- meta-chip
  [icon-name text]
  [:div {:class ["flex" "items-center" "gap-1" "px-2" "py-1" "rounded" "bg-surface-container" "font-label-sm" "text-label-sm" "text-on-surface-variant"]}
   (layout/icon ["text-[14px]"] icon-name)
   [:span text]])

(defn- active-session
  [curriculum]
  (when-let [{:keys [module] :as lesson} (active-lesson curriculum)]
    [:section {:class ["flex" "flex-col" "space-y-2"]}
     [:div {:class ["flex" "items-center" "gap-2"]}
      [:span {:class ["w-2" "h-2" "rounded-full" "bg-secondary-fixed" "animate-ping"]}]
      [:span {:class ["font-label-sm" "text-label-sm" "uppercase" "tracking-widest" "text-primary" "font-semibold"]} "Active Session"]]
     [:div {:class ["relative" "overflow-hidden" "rounded-xl" "bg-surface-container-low" "shadow-[0_4px_20px_rgba(0,0,0,0.5)]" "border" "border-border-gold/30" "flex" "flex-col"]}
      [:div {:class ["relative" "w-full" "h-36" "overflow-hidden" "bg-surface-container-lowest"]}
       [:img {:alt (:summary lesson)
              :class ["w-full" "h-full" "object-cover" "opacity-85"]
              :src (str "/assets/images/learn/" (:image module) ".jpg")}]
       [:div {:class ["absolute" "inset-0" "bg-gradient-to-t" "from-surface-container-low" "via-surface-container-low/40" "to-transparent"]}]
       [:div {:class ["absolute" "top-3" "left-3" "right-3" "flex" "items-center" "justify-between"]}
        [:span {:class ["px-2" "py-0.5" "rounded-full" "bg-surface-container-lowest/90" "backdrop-blur-md" "font-label-sm" "text-label-sm" "text-primary" "uppercase" "font-semibold" "border" "border-border-gold/30"]}
         (str "Module " (format "%02d" (:number module)) " • " (:track module))]
        [:div {:class ["inline-flex" "items-center" "gap-1" "px-2.5" "py-0.5" "rounded-full" "bg-surface-container-lowest/90" "backdrop-blur-md" "text-secondary" "border" "border-border-gold/30"]}
         [:span {:class ["font-label-sm" "text-label-sm" "font-semibold" "tracking-wider"]} (str "+" (:obj lesson) " OBJ")]]]]
      [:div {:class ["p-4" "flex" "flex-col" "space-y-3.5"]}
       [:div {:class ["space-y-1"]}
        [:span {:class ["font-label-sm" "text-label-sm" "text-text-muted" "uppercase" "tracking-wider" "block"]} (:title module)]
        [:h2 {:class ["font-headline-md" "text-headline-md" "text-text-primary" "leading-tight"]} (:title lesson)]
        [:p {:class ["font-body-md" "text-body-md" "text-on-surface-variant" "line-clamp-2"]} (:summary lesson)]]
       [:div {:class ["flex" "flex-wrap" "items-center" "gap-2" "pt-0.5"]}
        (meta-chip "schedule" (str (:minutes lesson) " min read"))
        (meta-chip "play_circle" (:kind lesson))
        (meta-chip "quiz" (str (:exam-prompts lesson) " Exam Prompts"))]
       [:div {:class ["pt-2" "flex" "flex-col" "space-y-2.5"]}
        [:div {:class ["flex" "items-center" "justify-between" "font-label-sm" "text-label-sm"]}
         [:span {:class ["text-text-muted"]}
          (str "Section " (:section lesson) " of " (:sections lesson) " (" (:progress lesson) "% Completed)")]
         [:span {:class ["text-primary" "font-semibold"]} "Quiz Available"]]
        [:div {:class ["w-full" "h-1.5" "rounded-full" "bg-surface-container-highest" "overflow-hidden"]}
         [:div {:class ["h-full" "rounded-full" "bg-primary-container" "shadow-[0_0_8px_rgba(245,194,66,0.4)]" ]
                :style (str "width: " (:progress lesson) "%;")}]]
        [:a {:href (data/lesson-href (:slug module) (:slug lesson))
             :class ["w-full" "mt-1" "py-3" "px-4" "rounded-lg" "bg-primary-container" "text-on-primary" "font-label-md" "text-label-md" "font-semibold" "tracking-wider" "uppercase" "flex" "items-center" "justify-center" "gap-2" "shadow-[0_0_16px_rgba(245,194,66,0.25)]" "hover:bg-primary" "transition-all" "active:scale-[0.98]"]}
         [:span "Resume Lesson"]
         (layout/icon ["text-[18px]"] "arrow_forward")]]]]]))

(defn curriculum-list
  [curriculum]
  (into [:div]
        (for [{:keys [slug title lessons]} (:modules curriculum)]
          [:section
           [:h2 title]
           (into [:ul]
                 (for [lesson lessons
                       :when (not= :locked (:status lesson))]
                   [:li [:a {:href (data/lesson-href slug (:slug lesson))} (:title lesson)]]))])))

(defn present
  [curriculum]
  {:content [:div
             (hero)
             (domain-pills curriculum)
             (status-filter curriculum)
             (active-session curriculum)
             (curriculum-list curriculum)]})

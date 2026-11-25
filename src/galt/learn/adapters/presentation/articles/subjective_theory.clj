(ns galt.learn.adapters.presentation.articles.subjective-theory
  (:require [galt.core.views.new-layout :as layout]))

(def ^:private section-heading
  ["font-headline-md" "text-primary" "font-serif" "text-[24px]" "sm:text-[26px]"
   "leading-[34px]" "font-semibold" "tracking-tight"])

(def ^:private paragraph
  ["font-body-lg" "text-body-lg" "text-on-surface" "leading-relaxed"])

(def body
  [[:article {:class ["flex" "flex-col" "gap-4"] :id "classical-fallacy"}
    [:h2 {:class section-heading} "1. The Classical Fallacy & Cost of Production"]
    [:p {:class paragraph}
     "For nearly a century following Adam Smith’s "
     [:em "The Wealth of Nations"]
     ", political economists remained trapped in an objective quagmire. David Ricardo, and subsequently Karl Marx, asserted that the exchange value of an economic good corresponds directly to the socially necessary labor-time crystallized within it."]
    [:p {:class paragraph}
     "This formulation failed in the face of elementary empirical observation: an artisan may expend one hundred hours carving a monument of worthless granite that nobody desires, while a traveler may unearth an uncut diamond in seconds and command sovereign wealth. Costs incurred in production do not retrospectively endow an object with worth."]
    [:div {:class ["my-4" "p-5" "rounded-lg" "bg-surface-container-low" "border-l-2" "border-primary-container" "shadow-md" "relative" "overflow-hidden"]}
     [:div {:class ["flex" "flex-col" "gap-2" "pl-2"]}
      [:span {:class ["font-label-sm" "text-label-sm" "uppercase" "tracking-widest" "text-primary" "font-semibold" "flex" "items-center" "gap-1.5"]}
       (layout/icon ["text-body-sm" "text-secondary"] "lightbulb")
       "Axiom of Subjectivity"]
      [:p {:class ["font-headline-md" "text-primary" "font-serif" "italic" "leading-relaxed" "text-[19px]" "sm:text-[21px]"]}
       "“Value is not inherent in goods; it does not reside in the hours of toil required to fabricate them. Value is a judgment formed solely in the economizing mind.”"]]]]
   [:article {:class ["flex" "flex-col" "gap-4"] :id "menger-orders"}
    [:h2 {:class section-heading} "2. Goods of First and Higher Orders"]
    [:p {:class paragraph}
     "In his 1871 breakthrough, Carl Menger established the causal ladder of production. Goods are not homogenous lumps of physical matter; they exist in structural orders corresponding to their distance from the consumer’s direct gratification:"]
    [:ul {:class ["flex" "flex-col" "gap-2" "pl-4" "text-on-surface-variant" "font-body-md" "text-body-lg"]}
     [:li {:class ["flex" "items-start" "gap-2"]}
      (layout/icon ["text-secondary" "text-body-md"] "arrow_right")
      [:span
       [:strong {:class ["text-primary"]} "First-Order Goods:"]
       " Consumable items that directly alleviate human want (e.g., baked bread on the table)."]]
     [:li {:class ["flex" "items-start" "gap-2"]}
      (layout/icon ["text-secondary" "text-body-md"] "arrow_right")
      [:span
       [:strong {:class ["text-primary"]} "Second-Order Goods:"]
       " Tools and ingredients directly adjacent to final products (e.g., flour, baking ovens)."]]
     [:li {:class ["flex" "items-start" "gap-2"]}
      (layout/icon ["text-secondary" "text-body-md"] "arrow_right")
      [:span
       [:strong {:class ["text-primary"]} "Higher-Order Goods:"]
       " Raw wheat fields, tractor assemblies, energy grids, and specialized capital equipment."]]]
    [:div {:class ["my-4" "p-5" "rounded-xl" "bg-surface-container" "shadow-md" "flex" "flex-col" "gap-4"]}
     [:div {:class ["flex" "items-center" "justify-between"]}
      [:span {:class ["font-label-sm" "text-label-sm" "tracking-wider" "uppercase" "text-primary" "font-semibold"]} "Analytical Doctrine Comparison"]
      [:span {:class ["font-label-sm" "text-label-sm" "text-on-surface-variant"]} "Methodological Divergence"]]
     [:div {:class ["grid" "grid-cols-1" "sm:grid-cols-2" "gap-3"]}
      [:div {:class ["p-4" "rounded-lg" "bg-surface-container-low" "flex" "flex-col" "gap-2.5"]}
       [:span {:class ["font-label-sm" "text-label-sm" "text-status-error" "uppercase" "tracking-wider" "font-semibold"]} "Labor Theory (Marxian / Classical)"]
       [:p {:class ["font-body-md" "text-body-md" "text-on-surface-variant" "leading-relaxed"]}
        "Value originates at the source of production: past human labor hours determine the baseline exchange value of goods downstream."]
       [:div {:class ["mt-2" "text-on-surface-variant" "font-label-sm" "text-label-sm" "flex" "items-center" "gap-1"]}
        (layout/icon ["text-body-md" "text-status-error"] "close")
        "Direction: Input → Output (Flawed)"]]
      [:div {:class ["p-4" "rounded-lg" "bg-surface-container-high" "flex" "flex-col" "gap-2.5"]}
       [:span {:class ["font-label-sm" "text-label-sm" "text-secondary" "uppercase" "tracking-wider" "font-semibold"]} "Marginal Utility (Austrian School)"]
       [:p {:class ["font-body-md" "text-body-md" "text-on-surface" "leading-relaxed"]}
        "Value originates in the subjective judgment of the end consumer; value is imputed backwards up the capital production chain."]
       [:div {:class ["mt-2" "text-secondary" "font-label-sm" "text-label-sm" "flex" "items-center" "gap-1"]}
        (layout/icon ["text-body-md" "text-secondary"] "check")
        "Direction: Final Consumer → Capital Chain"]]]]]
   [:article {:class ["flex" "flex-col" "gap-4"] :id "marginal-unit"}
    [:h2 {:class section-heading} "3. The Marginal Unit: Diamond-Water Paradox Resolved"]
    [:p {:class paragraph}
     "Why is potable water — indispensable for biological survival — exchanged for a pittance, while diamonds — frivolous ornamental stones — command immense fortresses of wealth? Adam Smith noticed this anomaly but could not formalize the answer."]
    [:div {:class ["p-5" "rounded-xl" "bg-surface-container-lowest" "shadow-sm" "flex" "flex-col" "gap-4"]}
     [:div {:class ["flex" "items-center" "justify-between"]}
      [:span {:class ["font-label-md" "text-label-md" "text-primary" "font-semibold" "tracking-wider" "uppercase"]} "Marginal Utility Schedule (5 Buckets of Water)"]
      [:span {:class ["font-label-md" "text-label-md" "text-secondary" "font-medium"]} "Interactive Axiom"]]
     [:div {:class ["flex" "flex-col" "gap-3" "font-body-md" "text-body-md"]}
      [:div {:class ["flex" "items-center" "justify-between" "p-3" "rounded" "bg-surface-container"]}
       [:span {:class ["font-medium" "text-primary"]} "Bucket 1: Direct Hydration (Life Preservation)"]
       [:span {:class ["font-label-md" "text-label-md" "text-secondary" "font-semibold"]} "Rank I (Crucial)"]]
      [:div {:class ["flex" "items-center" "justify-between" "p-3" "rounded" "bg-surface-container"]}
       [:span {:class ["font-medium" "text-on-surface"]} "Bucket 2: Livestock Maintenance"]
       [:span {:class ["font-label-md" "text-label-md" "text-on-surface" "font-semibold"]} "Rank II (High)"]]
      [:div {:class ["flex" "items-center" "justify-between" "p-3" "rounded" "bg-surface-container"]}
       [:span {:class ["font-medium" "text-on-surface-variant"]} "Bucket 3: Crop Irrigation"]
       [:span {:class ["font-label-md" "text-label-md" "text-on-surface-variant" "font-semibold"]} "Rank III (Moderate)"]]
      [:div {:class ["flex" "items-center" "justify-between" "p-3" "rounded" "bg-surface-container"]}
       [:span {:class ["font-medium" "text-on-surface-variant"]} "Bucket 4: Domestic Hygiene & Cleansing"]
       [:span {:class ["font-label-md" "text-label-md" "text-on-surface-variant" "font-semibold"]} "Rank IV (Low)"]]
      [:div {:class ["flex" "items-center" "justify-between" "p-3" "rounded" "bg-surface-container-high"]}
       [:span {:class ["font-medium" "text-primary-fixed-dim"]} "Bucket 5: Ornamental Fountain (Marginal Unit)"]
       [:span {:class ["font-label-md" "text-label-md" "text-primary" "font-semibold"]} "Rank V (Marginal)"]]]
     [:p {:class ["font-body-md" "text-body-md" "text-on-surface-variant" "leading-relaxed"]}
      "If one bucket is spilled, the economizing actor does not cease drinking; they abandon the fifth, lowest-ranked gratification. Thus, any individual unit of an identical stock is valued strictly according to the utility of the marginal unit."]]
    [:blockquote {:class ["my-6" "pl-6" "border-l-2" "border-secondary" "font-serif" "italic" "text-primary" "text-[20px]" "sm:text-[22px]" "leading-relaxed"]}
     "“Value is therefore nothing inherent in goods, no property of them, but merely the importance that we first attribute to the satisfaction of our needs.”"
     [:footer {:class ["mt-2" "font-label-md" "text-label-md" "text-on-surface-variant" "not-italic" "font-medium"]}
      "— Carl Menger, Principles of Economics (1871)"]]]
   [:article {:class ["flex" "flex-col" "gap-4"] :id "imputation-theory"}
    [:h2 {:class section-heading} "4. Imputation Theory (Zurechnung): Factor Pricing"]
    [:p {:class paragraph}
     "If final goods derive value subjectively, how do land, raw iron, and server clusters obtain market prices? Through Menger’s doctrine of Imputation ("
     [:em {:class ["text-primary"]} "Zurechnung"]
     "), later refined by Friedrich von Wieser. The prospective subjective utility of the consumer good is imputed inversely upon the factors of production necessary to materialize it."]
    [:p {:class paragraph}
     "A copper mine holds zero intrinsic economic value in isolation; its valuation is an imputation of future consumer preferences for electronic microchips, architectural fixtures, and energy grids."]]])

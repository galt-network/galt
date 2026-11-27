# Learn Pages in the New Design

**Goal:** Put the two Learn pages from the Stitch design online: the curriculum page at `/learn` and the lesson page at `/learn/:module-slug/:lesson-slug`, both in a new Tailwind v4 layout. Existing Bulma pages stay as they are, apart from one new link in their navigation bar.

**Architecture:** A compiled Tailwind v4 stylesheet carries the design tokens. A self-contained layout (`galt.core.views.new-layout`) is used only by the new pages. The `galt.learn` module serves both pages from a static EDN curriculum.

**Tech Stack:** Clojure, Hiccup, Reitit, Tailwind CSS 4.3.5 (`@tailwindcss/cli`), Datastar 1.0.2.

**Spec:** `docs/superpowers/specs/2026-11-24-learn-pages-design.md`

**Reference files (read them, never edit them):** `docs/design/learn/` holds `learn-mobile.html`, `learn-desktop.html`, `lesson.html`, `DESIGN.md` and `tailwind.css`. Design artwork is already in `resources/public/images/learn/`.

**Before this plan is queued:** `docs/design/learn/`, `resources/public/images/learn/`, this plan, the spec and `LLM_CODE_STYLE.md` must be committed and pushed to `master`, because the tasks read those files.

**Execution:** work the tasks in order, one commit per task, ticking the boxes of a task in its own commit.

## Global Constraints

- **Design source of truth** is `docs/design/learn/*.html`. Text (headings, labels, descriptions) is copied verbatim, including its spelling.
- **Porting rules** for every piece of markup:
  - Tailwind classes go in `:class` as a vector of strings: `[:div {:class ["flex" "gap-2"]}]`.
  - Write complete static class names only. Never build a class name at runtime; the Tailwind scanner has to see it.
  - Replace the arbitrary colour `[#f5c242]` with the token `primary-container` (`text-[#f5c242]` becomes `text-primary-container`, `bg-[#f5c242]` becomes `bg-primary-container`). Other arbitrary values are copied as they are.
  - Long or repeated class lists become a named constant (`def`) or a small function.
  - Use `galt.core.views.new-layout/icon` for Material Symbols icons; do not repeat the `material-symbols-outlined` span.
  - Let Hiccup escape `&` and other characters; never paste pre-escaped text.
- **Never port** these parts of the reference HTML: anchors with `href="#"` (keep their text in a non-link element), the theme toggle, the `html.light` style block and the theme script, the search field, the fake audio-player script, the pill-toggle script, the Tailwind Play CDN `<script>`, the inline `tailwind.config`, and images from `lh3.googleusercontent.com`.
- **Dark theme only.** No light-theme code and no `dark:` variants (the design uses none).
- **Both routes are public:** no `:min-role`.
- **Dependencies:** Tailwind is pinned to `4.3.5` in `package.json`; add no other npm package and no Clojure dependency.
- **Generated CSS** (`resources/public/css/tailwind.css`) is never committed. Run `npm run css` at the end of every task that adds or changes classes.
- **Images** are referenced as `/assets/images/learn/<module :image>.jpg`.
- **Exact names:** namespace `galt.core.views.new-layout` in `src/galt/core/views/new_layout.clj`; module `src/galt/learn/`; curriculum file `resources/learn/curriculum.edn`; route names `:learn` and `:learn/lesson`.
- **Old layout untouched:** do not edit `src/galt/core/views/layout.clj` or any Bulma page.
- **Clojure style:** follow `LLM_CODE_STYLE.md`.
- **Running tests:** use the clojure-mcp `clojure_eval` tool. A namespace is tested with
  ```clojure
  (do (require '<namespace> :reload) (clojure.test/run-tests '<namespace>))
  ```
  A run fails when the summary reports non-zero `:fail` or `:error`, or when the `require` throws.

## Review Focus

Each point is covered by the test named in brackets.

1. `/learn/nope/nope` and a lesson slug under the wrong module return 404 with "Lesson not found" and a link to `/learn`; a request without path parameters returns 404 and throws nothing. [`unknown-lesson-is-not-found`]
2. A listed lesson that has no article yet returns 200 and says "Lesson text coming soon.". [`lesson-without-article-says-so`]
3. An anonymous visitor gets 200 on `/learn` and a link to `/members/login`; a logged-in member sees their name and a link to `/members/profile/me`. [`learn-page-links-every-unlocked-lesson`, `member-sees-name-and-profile-link`]
4. Every unlocked lesson is linked from `/learn`; locked lessons are shown but never linked. [`learn-page-links-every-unlocked-lesson`, `locked-lessons-are-not-links`]
5. The curriculum data is complete: 3 modules, 9 lessons, unique slugs and titles, a summary for every unlocked lesson. [`curriculum-data-is-consistent`]
6. The new pages are a dark Tailwind page without Bulma or `href="#"`, and their navigation links only to `/`, `/connect`, `/learn`, `/events` and the profile or login page. [`shell-is-the-dark-tailwind-page`, `navigation-links-only-to-existing-pages`]
7. The curriculum page has separate mobile and desktop markup. [`learn-page-has-mobile-and-desktop-markup`]
8. The old layout is unchanged and its tests still pass. [Task 9 full check]

Human-only checks are listed under "For human review" at the end.

### Task 1: Tailwind build and design tokens

**Files:**
- Create: `package.json`, `resources/css/tailwind.css`
- Generated by `npm install` (commit it): `package-lock.json`
- Modify: `.gitignore`, `.dockerignore`, `Dockerfile`

- [x] Step 1: Create `package.json` with exactly this content:
  ```json
  {
    "name": "galt",
    "private": true,
    "scripts": {
      "css": "tailwindcss -i resources/css/tailwind.css -o resources/public/css/tailwind.css --minify",
      "css:watch": "tailwindcss -i resources/css/tailwind.css -o resources/public/css/tailwind.css --watch"
    },
    "devDependencies": {
      "@tailwindcss/cli": "4.3.5",
      "tailwindcss": "4.3.5"
    }
  }
  ```
- [x] Step 2: Run `npm install`. Check: `npm ls --depth=0` lists `@tailwindcss/cli@4.3.5` and `tailwindcss@4.3.5`, and `package-lock.json` exists.
- [x] Step 3: Copy the design tokens: `cp docs/design/learn/tailwind.css resources/css/tailwind.css`. Do not edit the copy.
- [x] Step 4: Build once: `npm run css`. Check: `grep -c -- '--color-canvas-base:#0a0a0c' resources/public/css/tailwind.css` and `grep -c -- '--text-body-md:16px' resources/public/css/tailwind.css` both print a number of 1 or more.
- [x] Step 5: Append these two lines to `.gitignore` (skip a line that is already there):
  ```
  node_modules
  resources/public/css/tailwind.css
  ```
  Check: `git status --short` shows neither `node_modules` nor `resources/public/css/tailwind.css`.
- [x] Step 6: In `.dockerignore` add `node_modules` on its own line after `target`.
- [x] Step 7: In `Dockerfile`, add a CSS stage directly after the `globo` stage (after the line ` && npx shadow-cljs release globo` and one blank line):
  ```dockerfile
  # Tailwind is compiled here so the generated CSS stays out of git.
  FROM node:22-alpine AS css
  WORKDIR /css
  COPY package.json package-lock.json ./
  RUN npm ci
  COPY resources/css ./resources/css
  COPY src ./src
  RUN npm run css
  ```
  Then, in the `build` stage, add this line directly after `COPY resources ./resources`:
  ```dockerfile
  COPY --from=css /css/resources/public/css/tailwind.css resources/public/css/tailwind.css
  ```
- [x] Step 8: Commit `package.json`, `package-lock.json`, `resources/css/tailwind.css`, `.gitignore`, `.dockerignore`, `Dockerfile` and the ticked boxes of this task.

**Commit:** `chore: add Tailwind v4 build with Learn design tokens`

### Task 2: Layout shell for the new pages

**Files:**
- Create: `src/galt/core/views/new_layout.clj`, `test/galt/core/views/new_layout_test.clj`
- Modify: `src/galt/core/infrastructure/web/helpers.clj`, `src/galt/core/system.clj` (`:route-deps` only)

**Interfaces:**
- `new-layout/main-layout [model]` returns the Hiccup for a whole page. Model keys: `:page-title`, `:path`, `:user` (`{:name :avatar :href}`), `:content`.
- `new-layout/content [hiccup]` wraps page content in `<main>`.
- `new-layout/icon [classes name]` returns a Material Symbols span.
- `web-helpers/with-new-layout [deps handler]` injects `:layout`, which merges its argument into the base model and renders `new-layout/main-layout`.

- [x] Step 1: Create `test/galt/core/views/new_layout_test.clj`:
  ```clojure
  (ns galt.core.views.new-layout-test
    (:require [clojure.string :as str]
              [clojure.test :refer [deftest is]]
              [galt.core.infrastructure.web.helpers :as web-helpers]
              [galt.core.views.new-layout :as new-layout]))

  (def ^:private anonymous-user
    {:name "Login" :avatar "/assets/images/profile-user-account.svg" :href "/members/login"})

  (defn- render
    [user]
    (web-helpers/render-html
      (new-layout/main-layout {:page-title "Learn" :path "/learn" :user user :content [:p "Hello"]})))

  (deftest shell-is-the-dark-tailwind-page
    (let [html (render anonymous-user)]
      (is (str/starts-with? html "<html class=\"dark\" lang=\"en\">"))
      (is (str/includes? html "<title>Learn | Galt</title>"))
      (is (str/includes? html "href=\"/assets/css/tailwind.css\""))
      (is (str/includes? html "datastar"))
      (is (str/includes? html "<main"))
      (is (not (str/includes? html "bulma")))
      (is (not (str/includes? html "href=\"#\"")))))

  (deftest navigation-links-only-to-existing-pages
    (let [html (render anonymous-user)
          tags (re-seq #"<a [^>]*>" html)
          tag-for (fn [href]
                    (some #(when (str/includes? % (str "href=\"" href "\"")) %) tags))]
      (doseq [href ["/" "/connect" "/learn" "/events" "/members/login"]]
        (is (string? (tag-for href))))
      (is (str/includes? (tag-for "/learn") "aria-current=\"page\""))
      (is (not (str/includes? (tag-for "/connect") "aria-current")))))

  (deftest member-sees-name-and-profile-link
    (let [html (render {:name "Alice" :avatar "/a.png" :href "/members/profile/me"})]
      (is (str/includes? html "Alice"))
      (is (str/includes? html "href=\"/members/profile/me\""))))

  (deftest toast-is-driven-by-notification-signals
    (let [html (render anonymous-user)]
      (is (str/includes? html "data-class:hidden=\"!$notification-visible\""))
      (is (str/includes? html "data-text=\"$notification-text\""))))
  ```
- [x] Step 2: Run the test namespace. Expect failure: `require` throws because `galt/core/views/new_layout` does not exist.
- [x] Step 3: Create `src/galt/core/views/new_layout.clj`. The header, tab bar and footer are ports of the `<header>`, the `nav` with `md:hidden fixed bottom-0` and the page `<footer>` in `docs/design/learn/lesson.html`, changed as described in the comments below:
  ```clojure
  (ns galt.core.views.new-layout
    (:require [clojure.string :as str]
              [galt.core.infrastructure.version :as version]))

  (def ^:private nav-items
    [{:title "Connect" :href "/connect" :icon "hub" :tab-icon "hub"}
     {:title "Learn" :href "/learn" :icon "school" :tab-icon "auto_stories"}
     {:title "Events" :href "/events" :icon "calendar_month" :tab-icon "calendar_month"}])

  ;; inactive and active anchors of the header nav, as in lesson.html
  (def ^:private nav-link-classes
    ["relative" "flex" "items-center" "gap-2" "py-2" "text-sm" "font-medium" "tracking-wide"
     "text-text-muted" "hover:text-text-primary" "transition-all" "duration-200" "group"])

  (def ^:private nav-link-active-classes
    ["relative" "flex" "items-center" "gap-2" "py-2" "text-sm" "font-semibold" "tracking-wide"
     "text-primary-container" "drop-shadow-[0_0_8px_rgba(245,194,66,0.35)]" "transition-all"
     "duration-200" "after:content-['']" "after:absolute" "after:bottom-0" "after:left-0"
     "after:right-0" "after:h-[2px]" "after:bg-primary-container" "after:rounded-full"
     "after:shadow-[0_0_10px_#f5c242]"])

  (def ^:private tab-classes
    ["flex" "flex-col" "items-center" "gap-1" "min-h-11" "text-text-muted"
     "hover:text-text-primary" "transition-colors"])

  (defn icon
    [classes name]
    [:span {:class (conj classes "material-symbols-outlined")} name])

  (defn content
    [hiccup]
    [:main {:id "content"
            :class ["flex-1" "w-full" "pt-16" "pb-28" "md:pb-8" "bg-canvas-base"]}
     hiccup])

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
       (icon ["text-[18px]" (if active? "text-primary-container" "text-text-muted group-hover:text-primary")]
             icon-name)
       [:span title]]))

  ;; the search field, theme toggle and rank pill of the reference are left out
  (defn- header
    [model]
    [:header {:class ["fixed" "top-0" "left-0" "w-full" "z-50" "bg-canvas-base/90"
                      "backdrop-blur-xl" "border-b" "border-border-subtle"]}
     [:div {:class ["h-16" "max-w-7xl" "mx-auto" "px-4" "sm:px-6" "lg:px-8" "flex"
                    "items-center" "justify-between" "gap-4"]}
      [:div {:class ["flex" "items-center" "gap-8" "lg:gap-10"]}
       (brand)
       (into [:nav {:class ["hidden" "md:flex" "items-center" "gap-6" "lg:gap-8"]}]
             (map (partial nav-link model) nav-items))]
      [:a {:class ["flex" "items-center" "gap-2" "pl-1" "border-l" "border-border-subtle"]
           :href (-> model :user :href)
           :title (-> model :user :name)}
       [:img {:class ["w-8" "h-8" "rounded-full" "object-cover" "ring-1" "ring-border-gold"]
              :src (-> model :user :avatar)
              :alt ""}]
       [:span {:class ["font-label-sm" "text-label-sm" "tracking-wide" "text-text-primary"]}
        (-> model :user :name)]]]])

  (defn- tab
    [model {:keys [title href tab-icon] :as item}]
    (let [active? (current? model item)]
      [:a (cond-> {:href href
                   :class (if active?
                            (conj tab-classes "relative" "text-primary-container"
                                  "drop-shadow-[0_0_8px_rgba(245,194,66,0.35)]")
                            tab-classes)}
            active? (assoc :aria-current "page"))
       (icon ["text-headline-sm"] tab-icon)
       [:span {:class ["font-label-sm" "text-label-sm" "tracking-wide"]} title]
       (when active?
         [:span {:class ["w-4" "h-[2px]" "bg-primary-container" "rounded-full"
                         "shadow-[0_0_6px_#f5c242]" "mt-0.5"]}])]))

  (defn- tab-bar
    [model]
    (into
      [:nav {:class ["md:hidden" "fixed" "bottom-0" "left-0" "right-0" "z-50"
                     "bg-canvas-base/95" "backdrop-blur-xl" "border-t" "border-border-subtle"
                     "px-2" "py-2" "pb-safe" "flex" "items-center" "justify-around"]}]
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
      (footer)]])
  ```
- [x] Step 4: In `src/galt/core/infrastructure/web/helpers.clj` add `[galt.core.views.new-layout :as new-layout]` to `:require` and add this function directly after `with-layout`:
  ```clojure
  (defn with-new-layout
    [deps handler]
    (fn [req]
      (let [layout-model (core-view-models/layout-model deps req)]
        (handler (merge deps
                        {:layout (partial layout-for-content
                                          new-layout/main-layout
                                          (assoc layout-model :user (-> layout-model :navbar :user)))})
                 req))))
  ```
- [x] Step 5: In `src/galt/core/system.clj`, inside the `:route-deps` map, add `:with-new-layout web-helpers/with-new-layout` on the line after `:with-layout web-helpers/with-layout`.
- [x] Step 6: Run `npm run css`, then run the test namespace. Expect: `Ran 4 tests containing 18 assertions. 0 failures, 0 errors.`
- [x] Step 7: Commit the four files and the ticked boxes of this task.

**Commit:** `feat: add Tailwind layout shell for new-design pages`

### Task 3: Static curriculum data

**Files:**
- Create: `resources/learn/curriculum.edn`, `src/galt/learn/adapters/curriculum.clj`, `test/galt/learn/adapters/curriculum_test.clj`

**Interfaces:**
- `curriculum/load-curriculum []` returns `{:modules [module …]}` read from the EDN file.
- `curriculum/lesson-href [module-slug lesson-slug]` returns `"/learn/<module-slug>/<lesson-slug>"`.
- `curriculum/find-lesson [curriculum module-slug lesson-slug]` returns the lesson with `:module` (its module), `:href` and `:next-lesson` (`{:title :href}` of the next lesson in curriculum order, `nil` after the last one), or `nil` when the pair does not exist.

- [x] Step 1: Create `test/galt/learn/adapters/curriculum_test.clj`:
  ```clojure
  (ns galt.learn.adapters.curriculum-test
    (:require [clojure.test :refer [deftest is]]
              [galt.learn.adapters.curriculum :as curriculum]))

  (deftest curriculum-data-is-consistent
    (let [modules (:modules (curriculum/load-curriculum))
          lessons (mapcat :lessons modules)]
      (is (= 3 (count modules)))
      (is (= 9 (count lessons)))
      (is (every? #(and (:slug %) (:title %)) modules))
    (is (every? #(and (:slug %) (:title %) (:status %)) lessons))
      (is (every? :summary (remove #(= :locked (:status %)) lessons)))
      (is (= (count modules) (count (set (map :slug modules)))))
      (is (= (count lessons) (count (set (map :slug lessons)))))
      (is (= (count lessons) (count (set (map :title lessons)))))))

  (deftest lesson-href-joins-the-slugs
    (is (= "/learn/praxeology/value-theory" (curriculum/lesson-href "praxeology" "value-theory"))))

  (deftest find-lesson-returns-lesson-with-navigation
    (let [lesson (curriculum/find-lesson (curriculum/load-curriculum) "praxeology" "value-theory")]
      (is (= "Intermediate Austrian Economics" (get-in lesson [:module :title])))
      (is (= "/learn/praxeology/value-theory" (:href lesson)))
      (is (= {:title "The Economic Calculation Problem" :href "/learn/praxeology/economic-calculation"}
             (:next-lesson lesson)))))

  (deftest last-lesson-has-no-next-lesson
    (is (nil? (:next-lesson (curriculum/find-lesson (curriculum/load-curriculum)
                                                    "legal-theory" "polycentric-law")))))

  (deftest unknown-lesson-is-nil
    (let [curriculum (curriculum/load-curriculum)]
      (doseq [[module-slug lesson-slug] [["nope" "nope"] ["praxeology" "sound-money"] [nil nil]]]
        (is (nil? (curriculum/find-lesson curriculum module-slug lesson-slug))))))
  ```
- [x] Step 2: Run the test namespace. Expect failure: `require` throws because `galt/learn/adapters/curriculum` does not exist.
- [x] Step 3: Create `resources/learn/curriculum.edn`:
  ```edn
  {:modules
   [{:slug "sound-architecture" :number 1 :track "Sound Architecture" :domain "Bitcoin Basics"
     :title "Bitcoin Basics & Absolute Scarcity" :obj 800 :obj-earned 800 :image "bitcoin-lattice"
     :lessons
     [{:slug "sound-money" :number 1 :topic "Sound Money"
       :title "Sound Money vs. Fiat Inflation" :status :completed
       :summary "Tracing monetary history from commodity standards to debt debasement, and how algorithmic predictability restores uncounterfeitable scarcity."
       :minutes 8 :clip-minutes 4 :score 100 :questions 5 :obj 200}
      {:slug "proof-of-work" :number 2 :topic "Proof of Work"
       :title "Proof of Work & Absolute Scarcity" :status :completed
       :summary "Connecting thermodynamic energy expenditure to unforgeable digital real estate: why SHA-256 anchors digital truth to physical reality."
       :minutes 11 :clip-minutes 6 :score 80 :questions 5 :obj 200}
      {:slug "supply-inelasticity" :number 3 :topic "Monetary Inelasticity"
       :title "The Halving & Supply Inelasticity" :status :completed
       :summary "Analyzing stock-to-flow ratios and the algorithmic quadrennial halving that enforces programmatic supply schedule regardless of price spikes."
       :minutes 14 :clip-minutes 8 :score 100 :questions 5 :obj 200}]}
    {:slug "praxeology" :number 2 :track "Praxeology" :domain "Austrian Econ"
     :title "Intermediate Austrian Economics" :obj 1150 :obj-earned 450 :image "menger-manuscripts"
     :lessons
     [{:slug "value-theory" :number 1 :topic "Value Theory"
       :title "The Subjective Theory of Value" :status :in-progress
       :summary "Deconstructing the classical labor theory of value through Carl Menger's prism of subjective satisfaction and marginal increments."
       :minutes 12 :progress 80 :section 4 :sections 5 :exam-prompts 5 :obj 350
       :kind "Lecture Clip (Audio/Visual)"}
      {:slug "economic-calculation" :number 2 :topic "Epistemology of Markets"
       :title "The Economic Calculation Problem" :status :available
       :summary "Why central planners inevitably fail: Ludwig von Mises' devastating critique demonstrating that without private property in capital goods, rational resource allocation is impossible."
       :minutes 18 :questions 4}
      {:slug "capital-structure" :number 3 :topic "Capital Structure"
       :title "Time Preference & Capital Structure" :status :locked :requires "2.2"
       :summary "Böhm-Bawerk's roundabout production structure: how artificial credit expansion distorts intertemporal prices and induces business cycles."
       :minutes 15 :questions 5}]}
    {:slug "legal-theory" :number 3 :track "Legal Theory" :domain "Natural Rights"
     :title "Natural Rights & Non-Aggression" :obj 900 :obj-earned 0 :image "scales-of-justice"
     :lessons
     [{:slug "self-ownership" :number 1 :topic "Jurisprudence"
       :title "Self-Ownership & Homesteading" :status :available
       :summary "The ethical axiomatic bedrock: deriving unalienable property titles from self-ownership and the transformation of unowned physical resources."
       :minutes 10 :questions 4 :obj 300 :kind "Audio Essay"}
      {:slug "nap" :number 2 :topic "Legal Ethics"
       :title "The Non-Aggression Axiom (NAP)" :status :locked :requires "3.1"
       :summary "Defining aggression versus legitimate defense, boundary enforcement, and proportional rectification in natural law tradition."
       :minutes 8 :questions 3 :obj 250}
      {:slug "polycentric-law" :number 3 :topic "Dispute Resolution"
       :title "Polycentric Law & Private Arbitration" :status :locked :requires "3.2"
       :summary "Historical precedent and game-theoretic mechanisms for competitive legal orders, insurance guarantors, and merchant law."
       :minutes 16 :questions 5 :obj 350}]}]}
  ```
- [x] Step 4: Create `src/galt/learn/adapters/curriculum.clj`:
  ```clojure
  (ns galt.learn.adapters.curriculum
    (:require [clojure.edn :as edn]
              [clojure.java.io :as io]
              [clojure.string :as str]))

  (defn load-curriculum
    []
    ;; shortcut: the curriculum file is parsed on every call, cache it when it grows
    (edn/read-string (slurp (io/resource "learn/curriculum.edn"))))

  (defn lesson-href
    [module-slug lesson-slug]
    (str "/learn/" module-slug "/" lesson-slug))

  (defn- lessons-in-order
    [curriculum]
    (for [{:keys [slug] :as module} (:modules curriculum)
          lesson (:lessons module)]
      (assoc lesson :module module :href (lesson-href slug (:slug lesson)))))

  (defn find-lesson
    [curriculum module-slug lesson-slug]
    (let [following (->> (lessons-in-order curriculum)
                         (drop-while #(not= (lesson-href module-slug lesson-slug) (:href %)))
                         seq)]
      (when-let [lesson (first following)]
        (assoc lesson :next-lesson (some-> (second following) (select-keys [:title :href]))))))
  ```
- [x] Step 5: Run the test namespace. Expect: `Ran 5 tests containing 16 assertions. 0 failures, 0 errors.`
- [x] Step 6: Commit the three files and the ticked boxes of this task.

**Commit:** `feat: add static curriculum data for Learn`

### Task 4: `/learn` route and navigation link

**Files:**
- Create: `src/galt/learn/external/routes.clj`, `src/galt/learn/adapters/handlers.clj`, `src/galt/learn/adapters/presentation/learn.clj`, `test/galt/learn/external/routes_test.clj`
- Modify: `src/galt/core/system.clj`, `src/galt/core/adapters/view_models.clj`, `test/galt/core/adapters/view_models_test.clj`

**Interfaces:**
- `learn-routes/router [deps]` returns a Reitit router built with `(:with-new-layout deps)`.
- `learn/show-learn [{:keys [render layout curriculum]} req]` returns `{:status 200 :body html}`.
- `presentation.learn/present [curriculum]` returns `{:content hiccup}`.

The page is a plain semantic list in this task. The design is ported onto it in Tasks 5, 6 and 8.

- [x] Step 1: Create `test/galt/learn/external/routes_test.clj`:
  ```clojure
  (ns galt.learn.external.routes-test
    (:require [clojure.string :as str]
              [clojure.test :refer [deftest is]]
              [galt.core.infrastructure.web.helpers :as web-helpers]
              [galt.learn.adapters.curriculum :as curriculum]
              [galt.learn.external.routes :as learn-routes]
              [galt.members.domain.member-repository :as mr]
              [galt.members.domain.user-repository :as ur]
              [reitit.ring :as rr]
              [ring.mock.request :as mock]))

  (def ^:private unlocked-lesson-hrefs
    ["/learn/sound-architecture/sound-money"
     "/learn/sound-architecture/proof-of-work"
     "/learn/sound-architecture/supply-inelasticity"
     "/learn/praxeology/value-theory"
     "/learn/praxeology/economic-calculation"
     "/learn/legal-theory/self-ownership"])

  (def ^:private locked-lesson-hrefs
    ["/learn/praxeology/capital-structure"
     "/learn/legal-theory/nap"
     "/learn/legal-theory/polycentric-law"])

  (defn- page
    ([path]
     (page path nil))
    ([path user]
     (let [deps {:user-repo (reify ur/UserRepository (find-user-by-id [_ _] user))
                 :member-repo (reify mr/MemberRepository (find-member-by-user-id [_ _] nil))
                 :asset-url identity
                 :render web-helpers/render-html
                 :with-new-layout web-helpers/with-new-layout
                 :curriculum curriculum/load-curriculum}
           request (cond-> (mock/request :get path)
                     user (assoc :session {:user-id 1}))]
       ((rr/ring-handler (learn-routes/router deps)) request))))

  (deftest learn-page-links-every-unlocked-lesson
    (let [{:keys [status body]} (page "/learn")]
      (is (= 200 status))
      (is (str/starts-with? body "<html class=\"dark\" lang=\"en\">"))
      (is (str/includes? body "href=\"/members/login\""))
      (doseq [href unlocked-lesson-hrefs]
        (is (str/includes? body (str "href=\"" href "\""))))))

  (deftest learn-page-lists-module-titles
    (let [body (:body (page "/learn"))]
      (doseq [title ["Bitcoin Basics &amp; Absolute Scarcity"
                     "Intermediate Austrian Economics"
                     "Natural Rights &amp; Non-Aggression"]]
        (is (str/includes? body title)))))

  (deftest member-sees-name-and-profile-link
    (let [{:keys [status body]} (page "/learn" {:name "Alice"})]
      (is (= 200 status))
      (is (str/includes? body "Alice"))
      (is (str/includes? body "href=\"/members/profile/me\""))))
  ```
- [x] Step 2: Run the test namespace. Expect failure: `require` throws because `galt/learn/external/routes` does not exist.
- [x] Step 3: Create `src/galt/learn/external/routes.clj`:
  ```clojure
  (ns galt.learn.external.routes
    (:require [galt.learn.adapters.handlers :as learn]
              [reitit.ring :as rr]))

  (defn router
    [deps]
    (let [with-layout (:with-new-layout deps)
          with-deps-layout (partial with-layout deps)]
      (rr/router
        [["/learn" {:id :learn :name :learn :get (with-deps-layout learn/show-learn)}]])))
  ```
- [x] Step 4: Create `src/galt/learn/adapters/handlers.clj`:
  ```clojure
  (ns galt.learn.adapters.handlers
    (:require [galt.learn.adapters.presentation.learn :as learn]
              [ring.util.http-status :as http-status]))

  (defn show-learn
    [{:keys [render layout curriculum]} _req]
    {:status http-status/ok
     :body (-> (curriculum) learn/present layout render)})
  ```
- [x] Step 5: Create `src/galt/learn/adapters/presentation/learn.clj`:
  ```clojure
  (ns galt.learn.adapters.presentation.learn
    (:require [galt.learn.adapters.curriculum :as data]))

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
    {:content (curriculum-list curriculum)})
  ```
- [x] Step 6: In `src/galt/core/system.clj`:
  - Add `[galt.learn.adapters.curriculum :refer [load-curriculum]]` to `:require` after the `[galt.connect.adapters.db-search-repository …]` line, and `[galt.learn.external.routes]` after `[galt.connect.external.routes]`.
  - In the `:route-deps` map add `:curriculum load-curriculum` on the line after `:with-new-layout web-helpers/with-new-layout`.
  - Add this component directly after the `:connect/routes` component (same shape, other namespace):
    ```clojure
         :learn/routes
         #::ds{:start
               (fn [{:keys [::ds/config]}]
                 (galt.learn.external.routes/router (:route-deps config)))
               :config
               {:route-deps (ds/ref [:app :route-deps])}}
    ```
  - In the `:router` component: add `learn-router (get-in config [:learn/routes])` after the `connect-router` binding, add `learn-router` as the last argument of `core.routes/merge-routers`, and add `:learn/routes (ds/ref [:app :learn/routes])` after the `:connect/routes` reference in its `:config`.
- [x] Step 7: Run `npm run css` and the routes test namespace. Expect: `Ran 3 tests containing 15 assertions. 0 failures, 0 errors.`
- [x] Step 8: Add this test to `test/galt/core/adapters/view_models_test.clj` and run that namespace. Expect failure: the second navbar item is still "Members".
  ```clojure
  (deftest navbar-links-learn-after-connect
    (let [items (:items (vm/navbar-model (stub-deps) {:reitit.core/match {:data {:id :learn}}}))]
      (is (= {:title "Learn" :href "/learn" :selected? true}
             (select-keys (second items) [:title :href :selected?])))))
  ```
- [x] Step 9: In `src/galt/core/adapters/view_models.clj`, add this item to `navbar-model` directly after the Connect item:
  ```clojure
  {:title "Learn" :href "/learn" :selected? (= :learn route-id)}
  ```
  Run the namespace again. Expect: `Ran 2 tests containing 2 assertions. 0 failures, 0 errors.`
- [x] Step 10: Smoke check, only when the dev system is already running in the nREPL (otherwise skip it and say so in the PR body): evaluate `(user/go!)`, then `curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8081/learn` prints `200` and `curl -s http://localhost:8081/learn | grep -c 'href="/learn"'` prints `1` or more.
- [x] Step 11: Commit the seven files and the ticked boxes of this task.

**Commit:** `feat: add /learn route backed by the static curriculum`

### Task 5: Learn page, mobile top sections

**Files:**
- Modify: `src/galt/learn/adapters/presentation/learn.clj`, `test/galt/learn/external/routes_test.clj`

**Interfaces:** `presentation.learn/present [curriculum]` still returns `{:content hiccup}`. New private functions: `hero`, `domain-pills`, `status-filter`, `active-session`. `curriculum-list` stays as it is until Task 6.

Port the markup from `docs/design/learn/learn-mobile.html`, from the comment `SOVEREIGN LEARNER MASTERY HERO BANNER` up to (not including) `CURRICULUM MODULES LIST`, under the porting rules of the Global Constraints. Keep the classes of the reference. The four blocks:

- `hero`: the hero banner. Its numbers come from the private map `member-stats` below. It contains no links.
- `domain-pills`: `TOPIC FILTER CAROUSEL PILLS`. Plain elements, not links. The label and its count are one text node: `"All Domains (9)"` (the number of lessons), then one pill per module with `(str (:domain module) " (" (count (:lessons module)) ")")`.
- `status-filter`: the Lesson Status filter (All, New, In Progress, Completed). Plain elements, label and count as one text node: `"All"`, `"New (2)"`, `"In Progress (1)"`, `"Completed (3)"`, each number counted from the lesson statuses `:available`, `:in-progress`, `:completed`.
- `active-session`: `RESUME / CURRENTLY ACTIVE LESSON FEATURED CARD` for the first lesson whose `:status` is `:in-progress`. Values come from the data: `"Module 02 • Praxeology"` (`:number` zero-padded, `:track`), module `:title`, `"Section 4 of 5"` (`:section`, `:sections`), `"80%"` and `"Progress 80%"` (`:progress`), lesson `:title` and `:summary`, `"12 min read"` (`:minutes`), `:kind`, `"5 Exam Prompts Pending"` (`:exam-prompts`), `"+350 OBJ"` (`:obj`). "Resume Lesson" is a link to `:href`.

- [x] Step 1: Add this test to `test/galt/learn/external/routes_test.clj` and run the namespace. Expect failure: the page shows neither the hero nor the active session.
  ```clojure
  (deftest learn-page-shows-hero-and-active-session
    (let [body (:body (page "/learn"))]
      (doseq [text ["Resume Lesson" "Sovereign Scholar" "Bitcoin Basics (3)" "Completed (3)" "Section 4 of 5"]]
        (is (str/includes? body text)))))
  ```
- [x] Step 2: Add the four private functions to `src/galt/learn/adapters/presentation/learn.clj`, require `[galt.core.views.new-layout :as layout]` for `layout/icon`, and change `present` to:
  ```clojure
  (def ^:private member-stats
    {:rank "Sovereign Scholar" :obj "4,850 OBJ" :progress 68 :cleared "14 / 22"
     :streak "18" :evaluations "14/14 (100%)" :domains "6 Domains"})
  ;; shortcut: placeholder member statistics copied from the design, replace when progress is stored

  (defn present
    [curriculum]
    {:content [:div
               (hero)
               (domain-pills curriculum)
               (status-filter curriculum)
               (active-session curriculum)
               (curriculum-list curriculum)]})
  ```
- [x] Step 3: Run `npm run css`, then the test namespace. Expect: `Ran 4 tests containing 20 assertions. 0 failures, 0 errors.`
- [x] Step 4: Commit the two files and the ticked boxes of this task.

**Commit:** `feat: port Learn hero, filters and active session for mobile`

### Task 6: Learn page, mobile curriculum path and closing call to action

**Files:**
- Modify: `src/galt/learn/adapters/presentation/learn.clj`, `test/galt/learn/external/routes_test.clj`

**Interfaces:** `present [curriculum]` is unchanged. `curriculum-list` is replaced by the ported curriculum path. New private functions: `module-section`, `lesson-row`, `closing-cta`.

Port the markup from `CURRICULUM MODULES LIST` to the end of `<main>` in `learn-mobile.html`: one section per module (`Module Header Card` followed by its lesson rows), then the closing call-to-action section. Data rules:

- Module header: `"Module 01 • Sound Architecture"` from `:number` (zero-padded) and `:track`, the module `:title`, then `"<done> / <total>"` as one text node (done = lessons with status `:completed` or `:in-progress`, total = all lessons), then the label as its own text node: `"PASSED (100%)"` when every lesson is completed, `"IN PROGRESS"` when at least one lesson is completed or in progress, otherwise `"COMPLETED"` (as in "0 / 3 COMPLETED"). OBJ texts use `:obj-earned` and `:obj`, as in the reference.
- Lesson row, by status:
  - `:completed`: minutes (`"8 min"`), clip (`"4 min clip"`), score (`"100% Score"`), "Review Article" as a link to `:href`, OBJ (`"Core • +200 OBJ"`), questions (`"Completed • 5/5"`).
  - `:in-progress`: progress and "Resume Lesson" as a link to `:href`.
  - `:available`: `"18 min read"`, questions, "Start Lesson" as a link to `:href`.
  - `:locked`: lock icon and `"Requires " (:requires lesson)`. It has no link.
- Thumbnail: `/assets/images/learn/<module :image>.jpg` with the lesson `:summary` as `alt`.
- `closing-cta`: the last section of the reference. A link to a page that does not exist becomes plain text (Global Constraints).

- [x] Step 1: Add these tests to `test/galt/learn/external/routes_test.clj` and run the namespace. Expect failure: `locked-lessons-are-not-links` fails on the title assertion, because the locked lesson is not rendered yet.
  ```clojure
  (deftest locked-lessons-are-not-links
    (let [body (:body (page "/learn"))]
      (doseq [href locked-lesson-hrefs]
        (is (not (str/includes? body (str "href=\"" href "\"")))))
      (is (str/includes? body "Time Preference &amp; Capital Structure"))))

  (deftest module-progress-is-derived-from-lessons
    (let [body (:body (page "/learn"))]
      (doseq [text ["3 / 3" "1 / 3" "1 / 3" "PASSED (100%)" "IN PROGRESS"]]
        (is (str/includes? body text)))))
  ```
- [x] Step 2: Replace `curriculum-list` with the ported curriculum path and add `closing-cta` after it in `present`.
- [x] Step 3: Run `npm run css`, then the test namespace. Expect: `Ran 6 tests containing 29 assertions. 0 failures, 0 errors.`
- [x] Step 4: Commit the two files and the ticked boxes of this task.

**Commit:** `feat: port Learn curriculum path for mobile`

### Task 7: Lesson page

**Files:**
- Create: `src/galt/learn/adapters/presentation/lesson.clj`, `src/galt/learn/adapters/presentation/articles/subjective_theory.clj`
- Modify: `src/galt/learn/adapters/handlers.clj`, `src/galt/learn/external/routes.clj`, `test/galt/learn/external/routes_test.clj`

**Interfaces:**
- `presentation.lesson/present [lesson]` takes the result of `curriculum/find-lesson` and returns `{:page-title <lesson title> :content hiccup}`.
- `presentation.lesson/not-found []` returns `{:page-title "Lesson not found" :content hiccup}` with the heading "Lesson not found" and a link to `/learn`.
- `articles.subjective-theory/body` is a Hiccup vector: the reading column of the lesson.
- `learn/show-lesson [{:keys [render layout curriculum]} req]` returns 200 with the lesson page, or 404 with `not-found` when `find-lesson` returns `nil`.
- Route `"/learn/:module-slug/:lesson-slug"` with `:id :learn` and `:name :learn/lesson`.

- [x] Step 1: Add `[galt.learn.adapters.handlers :as handlers]` to the `:require` of `test/galt/learn/external/routes_test.clj`, add the tests below and run the namespace. Expect failure: `require` throws because `handlers/show-lesson` does not exist.
  ```clojure
  (deftest lesson-page-renders-the-article
    (let [{:keys [status body]} (page "/learn/praxeology/value-theory")]
      (is (= 200 status))
      (is (str/includes? body "The Subjective Theory of Value &amp; Marginal Utility"))
      (is (str/includes? body "href=\"/learn/praxeology/economic-calculation\""))
      (is (str/includes? body "href=\"/learn\""))))

  (deftest lesson-without-article-says-so
    (let [{:keys [status body]} (page "/learn/sound-architecture/sound-money")]
      (is (= 200 status))
      (is (str/includes? body "Lesson text coming soon."))))

  (deftest unknown-lesson-is-not-found
    (doseq [path ["/learn/nope/nope" "/learn/praxeology/sound-money"]]
      (let [{:keys [status body]} (page path)]
        (is (= 404 status))
        (is (str/includes? body "Lesson not found"))
        (is (str/includes? body "href=\"/learn\""))))
    (let [{:keys [status body]} (handlers/show-lesson {:render str
                                                       :layout identity
                                                       :curriculum curriculum/load-curriculum}
                                                      {})]
      (is (= 404 status))
      (is (str/includes? body "Lesson not found"))))
  ```
- [x] Step 2: Create `src/galt/learn/adapters/presentation/articles/subjective_theory.clj`. Port the block `Center Column: Longform Treatise Editorial` of `docs/design/learn/lesson.html` (the comments `Section 1` to `Section 4`, including the Illuminated Axiom Callout, the comparison table, the bar chart and the pull quote) into `(def body […])`. Its values stay as they are in the reference. The namespace is `galt.learn.adapters.presentation.articles.subjective-theory`.
- [x] Step 3: Create `src/galt/learn/adapters/presentation/lesson.clj`:
  ```clojure
  (ns galt.learn.adapters.presentation.lesson
    (:require [galt.learn.adapters.presentation.articles.subjective-theory :as subjective-theory]))

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
  ```
  Implement the seven private functions above `present` as ports of `docs/design/learn/lesson.html`, in this order and under the porting rules of the Global Constraints:
  - `status-ribbon`: `Top Breadcrumb & Status Ribbon`. The "Curriculum" link goes to `/learn`. The status text comes from `:status`: `"Completed"`, `(str "In Progress • " (:progress lesson) "%")`, `"Not Started"` or `(str "Locked • Requires " (:requires lesson))`.
  - `progress-dock`: `Progress & Audio Narration Dock`, without the `Sovereign Voice Audio Player`. The progress track is filled to 100 for `:completed`, to `:progress` for `:in-progress`, otherwise to 0.
  - `hero`: `Editorial Hero Section` with the module `:track` and `:title`, the lesson `:topic`, `:summary` and the module `:obj` where the reference shows them.
  - `metadata-strip`: `Treatise Telemetry Metadata Strip` with `"12 min read"` (`:minutes`), `"5 Questions"` (`:questions`) and `"+350 OBJ"` (`:obj`), each only when the lesson has the value.
  - `artwork`: `Editorial Media Artwork`. Image `/assets/images/learn/<module :image>.jpg` with the lesson `:summary` as `alt`.
  - `reading-column`: `12-Column Responsive Reading Layout`. Its content is `(articles (:slug lesson))`, or `[:p "Lesson text coming soon."]` when that is `nil`.
  - `completion-ribbon`: `Bottom Lesson Completion & Next Action Ribbon`. When `:next-lesson` is present, its title links to `(:href (:next-lesson lesson))`. The back link goes to `/learn`. Leave out `Client-side Audio Micro-interaction`.
- [x] Step 4: In `src/galt/learn/adapters/handlers.clj` add `[galt.learn.adapters.curriculum :as data]` and `[galt.learn.adapters.presentation.lesson :as lesson]` to `:require`, and add:
  ```clojure
  (defn show-lesson
    [{:keys [render layout curriculum]} req]
    (let [{:keys [module-slug lesson-slug]} (:path-params req)]
      (if-let [found (data/find-lesson (curriculum) module-slug lesson-slug)]
        {:status http-status/ok
         :body (-> found lesson/present layout render)}
        {:status http-status/not-found
         :body (-> (lesson/not-found) layout render)})))
  ```
- [x] Step 5: In `src/galt/learn/external/routes.clj` add the route to `rr/router`:
  ```clojure
  ["/learn/:module-slug/:lesson-slug"
   {:id :learn :name :learn/lesson :get (with-deps-layout learn/show-lesson)}]
  ```
- [x] Step 6: Run `npm run css`, then the test namespace. Expect: `Ran 9 tests containing 43 assertions. 0 failures, 0 errors.`
- [x] Step 7: Commit the five files and the ticked boxes of this task.

**Commit:** `feat: add lesson page in the new design`

### Task 8: Learn page, desktop layout

**Files:**
- Modify: `src/galt/learn/adapters/presentation/learn.clj`, `test/galt/learn/external/routes_test.clj`

**Interfaces:** `present [curriculum]` is unchanged. New private functions: `desktop-hero`, `desktop-filters`, `desktop-modules`, `desktop-cta`.

Port the children of `<main>` in `docs/design/learn/learn-desktop.html` under the porting rules of the Global Constraints: the hero card ("Curriculum Ledger"), the filter section (domain chips and status chips, without the search input), the module grid (`grid-cols-1 md:grid-cols-2 lg:grid-cols-3`) and the closing call to action. Data rules:

- Hero: the same `member-stats` values as the mobile hero.
- Chips: plain elements with label and count as one text node, same rules as Task 5 (All, In Progress (1), Completed (3), then one chip per module).
- Lesson cards: same status rules as Task 6. Unlocked cards link to `:href`; locked cards have a lock icon and no link.
- `desktop-cta`: a link to a page that does not exist becomes plain text.

- [x] Step 1: Add this test to `test/galt/learn/external/routes_test.clj` and run the namespace. Expect failure: there is no desktop markup yet.
  ```clojure
  (deftest learn-page-has-mobile-and-desktop-markup
    (let [body (:body (page "/learn"))]
      (is (str/includes? body "<div class=\"md:hidden\">"))
      (is (str/includes? body "<div class=\"hidden md:block\">"))
      (is (str/includes? body "Curriculum Ledger"))))
  ```
- [x] Step 2: Change `present` so that the mobile blocks sit in one wrapper and the desktop blocks in the other:
  ```clojure
  (defn present
    [curriculum]
    {:content [:div
               [:div {:class ["md:hidden"]}
                (hero)
                (domain-pills curriculum)
                (status-filter curriculum)
                (active-session curriculum)
                (curriculum-list curriculum)
                (closing-cta)]
               [:div {:class ["hidden" "md:block"]}
                (desktop-hero)
                (desktop-filters curriculum)
                (desktop-modules curriculum)
                (desktop-cta)]]})
  ```
- [x] Step 3: Run `npm run css`, then the test namespace. Expect: `Ran 10 tests containing 46 assertions. 0 failures, 0 errors.`
- [x] Step 4: Commit the two files and the ticked boxes of this task.

**Commit:** `feat: port Learn desktop layout`

### Task 9: Documentation and full check

**Files:**
- Modify: `PROJECT_SUMMARY.md`, `docs/development.md`, `AGENTS.md`, `CONTEXT.md`, `PROGRESS.md`

- [x] Step 1: In `docs/development.md`, add this section directly before the `## Release & deployment` heading:
  ```markdown
  ## Tailwind (new design)

  Tailwind CSS v4 compiles `resources/css/tailwind.css` to `resources/public/css/tailwind.css`. The generated file is not committed. Design reference: `docs/design/learn/`.

  - Build once: `npm install && npm run css`
  - Rebuild on change: `npm run css:watch`
  ```
- [x] Step 2: In `AGENTS.md`, add this section:
  ```markdown
  ## Tailwind (new design)

  The Learn pages use Tailwind CSS v4 with the layout in `src/galt/core/views/new_layout.clj`. Reference markup and tokens are in `docs/design/learn/`.

  - Tailwind classes go in `:class` as a vector of strings: `[:div {:class ["flex" "gap-2"]}]`.
  - Write complete static class names only. Never build class names at runtime; the Tailwind scanner must see them.
  - Move long or repeated class lists into named constants or small component functions.
  - Prefer `@theme` tokens over arbitrary values.
  - Keep the class order: layout, spacing, typography, colors, effects, states, responsive.
  - Use semantic, accessible markup. Avoid `@apply`.
  - Run `npm run css` after adding classes. The generated CSS is not committed.
  ```
- [x] Step 3: In `PROJECT_SUMMARY.md`, add:
  - under Domain Modules: `src/galt/learn/` — Learn pages. `adapters/curriculum.clj` reads the static `resources/learn/curriculum.edn` and finds lessons; `adapters/presentation/learn.clj` is the curriculum page, `adapters/presentation/lesson.clj` and `articles/` the lesson page; `external/routes.clj` has `/learn` and `/learn/:module-slug/:lesson-slug` (public). Both pages use `galt.core.views.new-layout`, not the Bulma layout.
  - under Key File Paths: `src/galt/core/views/new_layout.clj` (Tailwind layout for new-design pages, wired as `:with-new-layout`) and `resources/css/tailwind.css` (Tailwind v4 tokens, compiled to the uncommitted `resources/public/css/tailwind.css` with `npm run css`).
  - under Documentation: `docs/design/learn/` — exported Stitch screens, design tokens and `DESIGN.md` for the new design.
  - under Dependencies: a "Build" row with `@tailwindcss/cli` and `tailwindcss` 4.3.5 (npm dev dependencies, `package.json`).
- [x] Step 4: In `CONTEXT.md`, add the terms Curriculum (all modules), Module (a numbered group of lessons), Lesson (one readable unit at `/learn/<module>/<lesson>`) and OBJ (the point unit used by the design; placeholder for now).
- [x] Step 5: In `PROGRESS.md`, record the Learn pages, the new layout and the Tailwind build under the current state, and add a short session log entry that names the test namespaces and their result.
- [x] Step 6: Full check. Each command must give the stated result:
  - `git diff --stat origin/master -- src/galt/core/views/layout.clj` prints nothing.
  - `npm run css` succeeds and `git status --short` does not list `resources/public/css/tailwind.css` or `node_modules`.
  - `grep -c -- '--color-canvas-base:#0a0a0c' resources/public/css/tailwind.css` prints a number of 1 or more.
  - Run:
    ```clojure
    (do (require 'galt.core.views.new-layout-test :reload)
        (require 'galt.core.adapters.view-models-test :reload)
        (require 'galt.learn.adapters.curriculum-test :reload)
        (require 'galt.learn.external.routes-test :reload)
        (require 'galt.connect.adapters.handlers-test :reload)
        (require 'galt.events.adapters.handlers-test :reload)
        (clojure.test/run-tests
          'galt.core.views.new-layout-test
          'galt.core.adapters.view-models-test
          'galt.learn.adapters.curriculum-test
          'galt.learn.external.routes-test
          'galt.connect.adapters.handlers-test
          'galt.events.adapters.handlers-test))
    ```
    Expect `Ran 48 tests containing 132 assertions. 0 failures, 0 errors`.
- [x] Step 7: Commit the five documentation files and the ticked boxes of this task.

**Commit:** `docs: document the Learn pages and the Tailwind build`

## For human review

These need eyes or a Docker build. The agent does not do them and does not claim them.

- Compare `/learn` and `/learn/praxeology/value-theory` with the Stitch screens at 390 px (Learn mobile, Lesson) and 1280 px (Learn desktop, Lesson). Both pages are public, no login needed.
- At 390 px there is no horizontal scroll and the bottom tab bar covers no content. At 1280 px the header navigation and the footer are visible and the tab bar is hidden.
- Fonts and icons load from Google Fonts; lesson thumbnails and artwork load from `/assets/images/learn/`.
- The old pages look as before and their navigation bar shows "Learn".
- `docker build` succeeds with the new `css` stage.

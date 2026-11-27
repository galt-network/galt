(ns galt.learn.external.routes-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [galt.core.infrastructure.web.helpers :as web-helpers]
            [galt.learn.adapters.curriculum :as curriculum]
            [galt.learn.adapters.handlers :as handlers]
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

(deftest learn-page-shows-hero-and-active-session
  (let [body (:body (page "/learn"))]
    (doseq [text ["Resume Lesson" "Sovereign Scholar" "Bitcoin Basics (3)" "Completed (3)" "Section 4 of 5"]]
      (is (str/includes? body text)))))

(deftest locked-lessons-are-not-links
  (let [body (:body (page "/learn"))]
    (doseq [href locked-lesson-hrefs]
      (is (not (str/includes? body (str "href=\"" href "\"")))))
    (is (str/includes? body "Time Preference &amp; Capital Structure"))))

(deftest module-progress-is-derived-from-lessons
  (let [body (:body (page "/learn"))]
    (doseq [text ["3 / 3" "1 / 3" "1 / 3" "PASSED (100%)" "IN PROGRESS"]]
      (is (str/includes? body text)))))

(deftest member-sees-name-and-profile-link
  (let [{:keys [status body]} (page "/learn" {:name "Alice"})]
    (is (= 200 status))
    (is (str/includes? body "Alice"))
    (is (str/includes? body "href=\"/members/profile/me\""))))

(deftest learn-page-has-mobile-and-desktop-markup
  (let [body (:body (page "/learn"))]
    (is (str/includes? body "<div class=\"md:hidden space-y-4\">"))
    (is (str/includes? body "<div class=\"hidden md:block space-y-4\">"))
    (is (str/includes? body "Curriculum Ledger"))))

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

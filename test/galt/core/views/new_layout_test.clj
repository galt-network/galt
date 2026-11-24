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

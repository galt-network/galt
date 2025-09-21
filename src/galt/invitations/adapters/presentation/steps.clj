(ns galt.invitations.adapters.presentation.steps
  (:require
   [galt.core.adapters.presentation-helpers :refer [render-markdown]]
   [galt.core.views.components :refer [message]]
   [galt.members.adapters.presentation.qr-code :as presentation.qr-code]))

(defn step-navbar-element
  [step]
  [:li {:class [(when (:active? step) "is-active")]}
   [:a (:name step)]])

(defn steps-progress
  [model]
  [:div.columns
   [:div.column.is-offset-one-quarter.is-half
    [:nv.breadcrumb.has-arrow-separator.is-medium
     [:ul
      (map step-navbar-element model)]]]])

(def start-instructions
  "Now that you have an invitation the rest of the process of **becoming a member** fairly simple.
  It consists of:
  1. Reading and understanding these instructions
  2. Logging in using LNURL-auth with your Bitcoin Lightning wallet (no payment at this point)
  3. Paying the one time membership fee (approximately 900 SAT-s or equivalent to 1 USD
    - this serves both to fund the development of Galt and also to avoid bot and spam accounts
  4. Setting up your profile (name, avatar, description) - all voluntary

  You for the next step (Login) you can use one of the following Lightning wallets (list not conclusive):
  - [Phoenix](https://phoenix.acinq.co/) _(that seems to be one of the easiest to set up)_
  - [Alby](https://getalby.com/)
  - [Blixt](https://blixtwallet.github.io/)
  - [Zeus](https://zeusln.com/)
  - [Wallet of Satoshi](https://www.walletofsatoshi.com/)
  ")

(defn start
  [model]
  [:div
   (steps-progress (:steps model))
   [:h1.title.is-2 "Start"]
   [:div.content (render-markdown start-instructions)]
   [:a.button.is-medium.is-primary {:href (:next-step model)} "Go to next step"]])

(def login-instructions
  "Scan this QR code with your lightning app. You will be prompted to log in
  and if successful will automatically taken to the next page")

(defn login-success [{:keys [next-step]}]
  [:div
   (message {:title "Success!" :content "You have successfully logged in" :type :success})
   [:a.button.is-primary {:href next-step} "Go to the next step"]])

(defn login-area
  [content]
  [:div#login-area content])

(defn login
  [{:keys [steps lnurl status-poll-action]}]
  [:div
   (steps-progress steps)
   [:h1.title.is-2 "Login"]
   [:div.columns
    [:div.column.is-three-fifths.is-offset-one-fifth
     [:div {
            :data-on-interval__duration.3s status-poll-action
            }
      [:div.content (render-markdown login-instructions)]
      (login-area (presentation.qr-code/qr-code-img lnurl))]]]])

(defn payment
  [model]
  (let []
    [:div
     (presentation.qr-code/qr-code-img "Nothing here")]))

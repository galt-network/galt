(ns galt.members.adapters.views
  (:require
    [galt.core.adapters.presentation-helpers :refer [render-markdown]]
    [galt.core.views.components :refer [errors-list]]))


(def login-explanation
  "## Explanation
  LNURL-login will generate a QR code that you can scan with your Bitcoin Lightning wallet.
  Then your wallet application will sign a challenge with your private key and request a
  URL on GALT platform with the signed challenge and your public key.
  Then GALT will verify that it was your public key that was used to sign the challenge,
  which when successful will complete the login process. After this your public key will
  identify you as a user on the platform.

  No other data is asked from you in order to use the platform.")

(defn login-form
  [model]
  [:div.columns.is-centered
   [:div.column.is-four-fifths
    (when (:message model) (errors-list (:message model)))
    [:div.content (render-markdown login-explanation)]
    [:div.block.is-size-5.has-text-centered
     [:div {:id "login-action-description"} "Click the button to login with Bitcoin Lightning LNURL-auth"]]
    [:div {:id "login-area"}
     [:div.field.has-addons.has-addons-centered
      [:div.control
       [:button.button.is-primary
        {:data-on-click "@post('/members/login')"} "Login with LNURL"]]]]]])

(defn login-result-message
  [model]
  [:article.message {:class [(:message-class model)]}
   [:div.message-header
    [:p (:message-header model)]
    [:button.delete]]
   [:div.message-body (:message-body model)]])

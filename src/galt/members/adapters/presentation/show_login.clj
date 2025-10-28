(ns galt.members.adapters.presentation.show-login
  (:require
    [galt.core.adapters.presentation-helpers :refer [render-markdown]]
    [galt.members.adapters.presentation.qr-code :as presentation.qr-code]
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

(defn present
  [model]
  [:div.columns.is-centered {:data-init (:datastar-action model)
                             ; :data-on:signal-patch "@get('/payments/new')"
                             ; :data-on:signal-patch-filters "{include: /payment-status/}"
                             }
   [:div.column.is-four-fifths
    (when (:message model) (errors-list (:message model)))
    [:div.content (render-markdown login-explanation)]
    [:div.block.is-size-5.has-text-centered
     [:div {:id "login-action-description"} "Scan the QR code with your Bitcoin Lightning wallet"]]
    [:div {:id "login-area"}
     [:div#login-area
      [:div.level
       [:div.level-item (presentation.qr-code/qr-code-img (:lnurl model))]]]]]])

(defn login-result
  [model]
  [:div#login-area
   [:article.message {:class [(:message-class model)]}
    [:div.message-header
     [:p (:message-header model)]
     [:button.delete]]
    [:div.message-body (:message-body model)]]])

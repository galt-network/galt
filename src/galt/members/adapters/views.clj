(ns galt.members.adapters.views
  (:require
    [clj.qrgen]
    [markdown.core]
    [hiccup2.core]
    [galt.core.views.table :refer [table]]
    [galt.core.adapters.time-helpers :refer [short-format]])
  (:import
    [java.util Base64]))

(defn encode-base64 [to-encode]
  (.encodeToString (Base64/getEncoder) to-encode))

(defn group-tags
  [groups]
  (let [tags (map (fn [g]
                    [:a {:href (str "https://dev.galt.is/groups/" (:groups/id g))} [:span.tag (:groups/name g)]])
                  groups)
        groups-to-show 4
        remaining-count (- (count tags) groups-to-show)
        more-text (str "And " remaining-count " more...")]
    (if (> (count tags) groups-to-show)
      (conj [:div.tags] (take groups-to-show tags) [:span.tag more-text])
      [:div.tags tags])))

(defn link-to-profile
  [user]
  [:a {:href (:href user)} (:name user)])

(defn members-list
  [model]
  (table {:columns (:column-titles model)
          :rows (:users model)
          :column-processor {:user link-to-profile
                             :groups #(group-tags %)
                             :users/created-at #(short-format (.toLocalDateTime %))}}))

(defn qr-code
  [content]
  (->
    (clj.qrgen/from content :size [300 300])
    (clj.qrgen/as-bytes ,,,)
    (encode-base64 ,,,)))

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
  [_model]
  [:div.columns.is-centered
   [:div.column.is-four-fifths
    [:div.content (hiccup2.core/raw (markdown.core/md-to-html-string login-explanation))]
    [:div.block.is-size-5.has-text-centered
     [:div {:id "login-action-description"} "Click the button to login with Bitcoin Lightning LNURL-auth"]]
    [:div {:id "login-area"}
     [:div.field.has-addons.has-addons-centered
     [:div.control
      [:button.button.is-primary
       {:data-on-click "@post('/members/login')"} "Login with LNURL"]]]]]])

(defn qr-code-img
  [lnurl]
  [:div
   [:figure.image
    [:a {:href (str "lightning:" lnurl)}
     [:img {:src (str "data:image/png;base64," (qr-code lnurl))}]]]
    [:div.control.has-icons-right
     [:input {:type :text
              :class [:input]
              :value lnurl
              :size 50
              :read-only true
              :onclick "galt.copyInputToClipboard(this)"}]
     [:span {:class [:icon :is-small :is-right]}
      [:i.far.fa-clipboard ]]]])

(defn login-result-message
  [status result]
  (let [message-class {:ok :is-success :error :is-danger}
        message-content {:ok [:div "Your user name generated from your public key is "
                              [:strong (get-in result [:user :users/name])]]
                         :error "Try logging in again or if you think this is our error, please report it"}]
    [:article.message {:class [(message-class status)]}
     [:div.message-header
      [:p (:message result)]
      [:button.delete]]
     [:div.message-body (message-content status)]]))

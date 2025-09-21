(ns galt.members.adapters.presentation.qr-code
  (:require
    [clj.qrgen])
  (:import
    [java.util Base64]))

(defn encode-base64 [to-encode]
  (.encodeToString (Base64/getEncoder) to-encode))

(defn qr-code
  [content]
  (->
    (clj.qrgen/from content :size [400 400])
    (clj.qrgen/as-bytes ,,,)
    (encode-base64 ,,,)))

(defn qr-code-img
  [lnurl]
  [:div
   [:div {:class "is-flex is-justify-content-center"}
    [:figure.image
     [:a {:href (str "lightning:" lnurl)}
      [:img {:src (str "data:image/png;base64," (qr-code lnurl))}]]]]
    [:div.control.has-icons-right
     [:input {:type :text
              :class [:input]
              :value lnurl
              :size 50
              :read-only true
              :onclick "galt.copyInputToClipboard(this)"}]
     [:span {:class [:icon :is-small :is-right]}
      [:i.far.fa-clipboard ]]]])

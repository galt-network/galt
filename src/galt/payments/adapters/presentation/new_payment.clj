(ns galt.payments.adapters.presentation.new-payment
  (:require
   [galt.core.views.components :refer [errors-list]]
   [galt.core.views.datastar-helpers :refer [d*-backend-action]]
   [galt.members.adapters.presentation.qr-code :refer [qr-code-img]]))

(defn show-invoice-payable
  [model]
  [:div#payment-container
   [:h1.title.is-2 "Pay by scanning this QR code with your Lightning wallet"]
   (qr-code-img (:invoice-url model))
   [:pre (str model)]
   [:input {:type "text" :data-bind "payment-status"}]])

(defn show-invoice-paid
  [model]
  [:div#payment-container
   [:h1.title.is-1 "Invoice successfully paid"]
   [:a.button {:href (:return-to model)} "Return to your previous page"]])

(defn show-invoice-expired
  [model]
  [:div#payment-container
   [:h1.title.is-1 "Invoice expired"]])

(defn payment-container
  [model]
  [:div#payment-container
    (case (:invoice-status model)
      "created" (show-invoice-payable model)
      "unpaid" (show-invoice-payable model)
      "expired" (show-invoice-expired model)
      "paid" (show-invoice-paid model)
      :error (errors-list "Error processing the invoice" (:message model)))])

(defn refetch-action
  [return-to]
  (if return-to
    (d*-backend-action "/payments/new" :get {:return-to return-to})
    "@get('/payments/new')"))

(defn present
  [{:keys [datastar-action return-to] :as model}]
  [:div {:data-init datastar-action
         :data-on-signal-patch (refetch-action return-to)}
   (payment-container model)])

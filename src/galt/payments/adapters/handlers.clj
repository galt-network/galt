(ns galt.payments.adapters.handlers
  (:require
   [camel-snake-kebab.core :as csk]
   [camel-snake-kebab.extras :as cske]
   [galt.core.adapters.sse-helpers :refer [send! with-sse]]
   [galt.core.infrastructure.web.sse-connection-store :refer [get-connection]]
   [galt.core.views.datastar-helpers :refer [d*-backend-action]]
   [galt.payments.adapters.presentation.new-payment :as presentation.new-payment]
   [starfederation.datastar.clojure.api :as d*]))

(def last-invoice (atom nil))
(def last-req (atom nil))

(defn new-payment
  [{:keys [membership-payment-use-case layout render]} req]
  (reset! last-req req)
  (let [payment-type (get-in req [:params :payment-type])
        return-to (get-in req [:params :return-to])
        user-id (get-in req [:session :user-id])
        [status result] (membership-payment-use-case {:user-id user-id})
        _ (reset! last-invoice result)

        sse-connection-id (:label result)
        datastar-action (d*-backend-action "/datastar-sse" :post {:connection-id sse-connection-id})
        model {:invoice-url (:bolt-11 result)
               :datastar-action datastar-action
               :return-to return-to
               :invoice-status (if (= :error status) :error (:status result))
               :message (:message result)}]
    (if (d*/datastar-request? req)
      (with-sse req (fn [send!] (send! :html (presentation.new-payment/payment-container model))))
      {:status 200 :body (-> model presentation.new-payment/present layout render)})))

(defn cln-webhook
  "Called back by Core Lightning webhook with a JSON map of event-type => event-data"
  [{:keys [update-invoice-use-case]} req]
  (let [events (req :body)
        event-type-data (map (fn [[k v]] [k (cske/transform-keys csk/->kebab-case-keyword v)]) events)]
    (doseq [[type data] event-type-data]
      (case type
        "invoice_payment"
        (let [invoice (update-invoice-use-case {:label (:label data)})
              connection-id (:label invoice)]
          (send! (get-connection connection-id) :signals {:payment-status (:status invoice)}))))
    {:status 200 :body "OK"}))

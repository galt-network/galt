(ns galt.payments.adapters.handlers
  (:require
    [buddy.core.codecs :as codecs]
    [buddy.core.mac :as mac]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [clojure.walk :as walk]
    [galt.core.adapters.sse-helpers :refer [send! with-sse]]
    [galt.core.infrastructure.web.helpers :refer [read-json]]
    [galt.core.infrastructure.web.sse-connection-store :refer [get-connection]]
    [galt.core.views.datastar-helpers :refer [d*-backend-action]]
    [galt.payments.adapters.presentation.new-payment :as presentation.new-payment]
    [starfederation.datastar.clojure.api :as d*])
  (:import [java.nio.charset StandardCharsets]))

(defn new-payment
  [{:keys [membership-payment-use-case layout render]} req]
  (let [payment-type (get-in req [:params :payment-type])
        return-to (get-in req [:params :return-to])
        user-id (get-in req [:session :user-id])
        [status result] (membership-payment-use-case {:user-id user-id})
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

(defn request-bytes [req]
  (let [body (:body req)]
    (cond
      (bytes? body) body
      (string? body) (.getBytes ^String body StandardCharsets/UTF_8)
      (nil? body) (byte-array 0)
      :else (.readAllBytes ^java.io.InputStream (io/input-stream body)))))

(defn valid-signature? [secret raw-body header]
  (boolean
    (when (and (seq secret) (string? header) (str/starts-with? header "sha256="))
      (try
        (mac/verify raw-body
                    (codecs/hex->bytes (subs header 7))
                    {:key secret :alg :hmac+sha256})
        (catch Exception _ false)))))

(defn handle-btcpay-webhook
  [{:keys [update-invoice-use-case btcpay-webhook-secret]} raw-body header]
  (if-not (valid-signature? btcpay-webhook-secret raw-body header)
    {:status 401 :body "invalid signature"}
    (if-let [event (try
                     (walk/keywordize-keys (read-json (String. ^bytes raw-body StandardCharsets/UTF_8)))
                     (catch Exception _ nil))]
      (do
        (when (= "InvoiceSettled" (:type event))
          (when-let [label (get-in event [:metadata :orderId])]
            (when-let [invoice (update-invoice-use-case {:label label})]
              (when-let [connection (get-connection (:label invoice))]
                (send! connection :signals {:payment-status (:status invoice)})))))
        {:status 200 :body "OK"})
      {:status 400 :body "invalid json"})))

(defn btcpay-webhook [deps req]
  (handle-btcpay-webhook deps (request-bytes req) (get-in req [:headers "btcpay-sig"])))

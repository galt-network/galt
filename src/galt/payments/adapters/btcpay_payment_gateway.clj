(ns galt.payments.adapters.btcpay-payment-gateway
  (:require
    [clojure.walk :as walk]
    [failjure.core :as f]
    [galt.core.infrastructure.web.helpers :refer [->json read-json]]
    [galt.payments.domain.payment-gateway :refer [PaymentGateway]]
    [java-time.api :as jt]
    [org.httpkit.client :as http]))

(def ^:private status->galt
  {"New" "unpaid"
   "Processing" "unpaid"
   "Expired" "expired"
   "Invalid" "error"
   "Settled" "paid"})

(defn galt-status [status]
  (get status->galt status "error"))

(defn- secs->instant [secs]
  (jt/instant (* 1000 (long secs))))

(defn- sats->msat [amount]
  (long (* 1000 (bigdec amount))))

(defn- expiration-minutes [expiry-seconds]
  (long (Math/ceil (/ expiry-seconds 60.0))))

(defn create-invoice-body [{:keys [amount-msat label description expiry]}]
  (cond-> {:amount (str (quot amount-msat 1000))
           :currency "SATS"
           :metadata (cond-> {:orderId label}
                       (seq description) (assoc :itemDesc description))
           :checkout {:paymentMethods ["BTC-LN"]}}
    (and expiry (pos? expiry))
    (assoc-in [:checkout :expirationMinutes] (expiration-minutes expiry))))

(defn- ln-method [methods]
  (first (filter #(= "BTC-LN" (:paymentMethodId %)) methods)))

(defn galt-invoice [invoice methods]
  (let [ln (ln-method methods)
        status (galt-status (:status invoice))
        payment (last (sort-by :receivedDate (or (:payments ln) [])))]
    (cond-> {:label (get-in invoice [:metadata :orderId])
             :status status
             :amount-msat (sats->msat (:amount invoice))
             :amount-received-msat (sats->msat (or (:paidAmount invoice) "0"))}
      (:destination ln) (assoc :bolt-11 (:destination ln))
      (get-in ln [:additionalData :paymentHash]) (assoc :payment-hash (get-in ln [:additionalData :paymentHash]))
      (get-in ln [:additionalData :preimage]) (assoc :payment-preimage (get-in ln [:additionalData :preimage]))
      (:expirationTime invoice) (assoc :expires-at (secs->instant (:expirationTime invoice)))
      (= "paid" status) (assoc :paid-at (or (some-> (:receivedDate payment) secs->instant)
                                           (jt/instant))))))

(defn- assert-sats [invoice]
  (if (= "SATS" (:currency invoice))
    invoice
    (f/fail (str "expected SATS invoice, got " (:currency invoice)))))

(defn- whole-sats? [amount-msat]
  (and (number? amount-msat) (pos? amount-msat) (zero? (mod amount-msat 1000))))

(defn- parse-response [res]
  (cond
    (:error res) (f/fail (str "BTCPay request failed: " (:error res)))
    (not (<= 200 (:status res) 299)) (f/fail (str "BTCPay returned " (:status res)))
    :else res))

(defn- parse-body [raw]
  (when (seq raw) (walk/keywordize-keys (read-json raw))))

;; ok->> splices into fn forms instead of calling them, so steps are symbols.
(defn btcpay-request
  [base-url api-key {:keys [method path query body]}]
  (f/ok->> (f/try* @(http/request (cond-> {:url (str base-url path)
                                           :method method
                                           :headers {"Authorization" (str "token " api-key)
                                                     "Content-Type" "application/json"
                                                     "Accept" "application/json"}
                                           :query-params query}
                                    body (assoc :body (->json body)))))
           parse-response
           :body
           parse-body))

(defn- invoice-with-methods [request store-id invoice]
  (f/ok->> (request {:method :get
                     :path (str "/api/v1/stores/" store-id
                                "/invoices/" (:id invoice) "/payment-methods")})
           (galt-invoice invoice)))

(defn- first-by-label [request store-id invoices]
  (when-let [invoice (first invoices)]
    (f/ok->> (assert-sats invoice)
             (invoice-with-methods request store-id))))

(defrecord BtcpayPaymentGateway [request store-id]
  PaymentGateway
  (create-invoice [_ {:keys [amount-msat] :as params}]
    (if-not (whole-sats? amount-msat)
      (f/fail "amount-msat must be a positive whole number of sats")
      (f/ok->> (request {:method :post
                         :path (str "/api/v1/stores/" store-id "/invoices")
                         :body (create-invoice-body params)})
               assert-sats
               (invoice-with-methods request store-id))))
  (invoice-by-label [_ label]
    (f/ok->> (request {:method :get
                       :path (str "/api/v1/stores/" store-id "/invoices")
                       :query {:orderId label}})
             (first-by-label request store-id))))

(defn new-btcpay-payment-gateway
  ([request store-id]
   (->BtcpayPaymentGateway request store-id))
  ([base-url store-id api-key]
   (new-btcpay-payment-gateway (partial btcpay-request base-url api-key) store-id)))

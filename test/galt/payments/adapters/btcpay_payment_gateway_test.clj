(ns galt.payments.adapters.btcpay-payment-gateway-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [failjure.core :as f]
    [galt.payments.adapters.btcpay-payment-gateway :as gw]
    [galt.payments.domain.payment-gateway :as pg]
    [java-time.api :as jt]))

(def bolt11 "lnbcrt1test")

(def settled-invoice
  {:id "inv1"
   :status "Settled"
   :currency "SATS"
   :amount "960"
   :paidAmount "960"
   :expirationTime 1791641107
   :metadata {:orderId "lab"}})

(def settled-methods
  [{:paymentMethodId "BTC-LN"
    :destination bolt11
    :additionalData {:paymentHash "hash1" :preimage "pre1" :invoiceId "cln1"}
    :payments [{:id "p1" :receivedDate 1791640000 :value "960"}]}])

(def invoice-columns
  #{:label :amount-msat :amount-received-msat :status :bolt-11 :payment-hash
    :payment-preimage :expires-at :paid-at})

(deftest create-invoice-body-test
  (is (= {:amount "960"
          :currency "SATS"
          :metadata {:orderId "lab" :itemDesc "desc"}
          :checkout {:paymentMethods ["BTC-LN"]}}
         (gw/create-invoice-body {:amount-msat 960000 :label "lab" :description "desc"})))
  (is (= 2 (get-in (gw/create-invoice-body {:amount-msat 1000 :label "lab" :expiry 90})
                   [:checkout :expirationMinutes])))
  (is (not (contains? (:checkout (gw/create-invoice-body {:amount-msat 1000 :label "lab"}))
                      :expirationMinutes))))

(deftest galt-invoice-test
  (testing "settled lightning payment maps onto invoice columns"
    (let [invoice (gw/galt-invoice settled-invoice settled-methods)]
      (is (= "paid" (:status invoice)))
      (is (= 960000 (:amount-msat invoice)))
      (is (= 960000 (:amount-received-msat invoice)))
      (is (= bolt11 (:bolt-11 invoice)))
      (is (= "hash1" (:payment-hash invoice)))
      (is (= "pre1" (:payment-preimage invoice)))
      (is (= "lab" (:label invoice)))
      (is (= (jt/instant (* 1000 1791641107)) (:expires-at invoice)))
      (is (= (jt/instant (* 1000 1791640000)) (:paid-at invoice)))
      (is (empty? (remove invoice-columns (keys invoice))))))
  (testing "unpaid invoice does not null bolt11 or paid-at"
    (let [invoice (gw/galt-invoice
                    (assoc settled-invoice :status "New" :paidAmount "0")
                    [{:paymentMethodId "BTC-LN"
                      :destination bolt11
                      :additionalData {:paymentHash "hash1" :preimage nil}
                      :payments []}])]
      (is (= "unpaid" (:status invoice)))
      (is (= bolt11 (:bolt-11 invoice)))
      (is (not (contains? invoice :payment-preimage)))
      (is (not (contains? invoice :paid-at)))
      (is (= 0 (:amount-received-msat invoice)))))
  (testing "settled without a payment still sets paid-at"
    (is (instance? java.time.Instant
                   (:paid-at (gw/galt-invoice settled-invoice
                                              [{:paymentMethodId "BTC-LN" :payments []}]))))))

(defn fake-request [calls responses]
  (fn [call]
    (swap! calls conj call)
    (first (swap! responses rest))))

(deftest create-invoice-test
  (let [calls (atom [])
        responses (atom [nil settled-invoice settled-methods])
        gateway (gw/new-btcpay-payment-gateway (fake-request calls responses) "store1")
        result (pg/create-invoice gateway {:amount-msat 960000 :label "lab" :description "desc"})]
    (is (= {:method :post
            :path "/api/v1/stores/store1/invoices"
            :body (gw/create-invoice-body {:amount-msat 960000 :label "lab" :description "desc"})}
           (first @calls)))
    (is (= {:method :get :path "/api/v1/stores/store1/invoices/inv1/payment-methods"}
           (second @calls)))
    (is (= bolt11 (:bolt-11 result)))
    (is (instance? java.time.Instant (:expires-at result)))
    (is (not (contains? result :payment-secret)))
    (is (not (contains? result :created-index))))
  (let [calls (atom [])
        gateway (gw/new-btcpay-payment-gateway (fake-request calls (atom [])) "store1")]
    (is (f/failed? (pg/create-invoice gateway {:amount-msat 1500 :label "lab"})))
    (is (empty? @calls))))

(deftest btcpay-request-test
  (let [seen (atom nil)]
    (with-redefs [org.httpkit.client/request
                  (fn [req]
                    (reset! seen req)
                    (doto (promise)
                      (deliver (if (clojure.string/ends-with? (:url req) "/x")
                                 {:status 500 :body "no"}
                                 {:status 200 :body "{\"id\":\"inv1\",\"currency\":\"SATS\"}"}))))]
      (is (= {:id "inv1" :currency "SATS"}
             (gw/btcpay-request "http://btcpay" "k"
                                {:method :post
                                 :path "/api/v1/stores/s/invoices"
                                 :body {:amount "1"}})))
      (is (= "http://btcpay/api/v1/stores/s/invoices" (:url @seen)))
      (is (= "token k" (get-in @seen [:headers "Authorization"])))
      (is (= :post (:method @seen)))
      (is (f/failed?
            (gw/btcpay-request "http://btcpay" "k" {:method :get :path "/x"}))))))

(deftest invoice-by-label-test
  (let [calls (atom [])
        responses (atom [nil [settled-invoice] settled-methods])
        gateway (gw/new-btcpay-payment-gateway (fake-request calls responses) "store1")
        result (pg/invoice-by-label gateway "lab")]
    (is (= {:method :get
            :path "/api/v1/stores/store1/invoices"
            :query {:orderId "lab"}}
           (first @calls)))
    (is (= "paid" (:status result)))
    (is (= "pre1" (:payment-preimage result)))
    (is (empty? (remove invoice-columns (keys result)))))
  (let [gateway (gw/new-btcpay-payment-gateway (fn [_] []) "store1")]
    (is (nil? (pg/invoice-by-label gateway "missing"))))
  (let [gateway (gw/new-btcpay-payment-gateway
                  (fn [_] [(assoc settled-invoice :currency "USD")])
                  "store1")]
    (is (f/failed? (pg/invoice-by-label gateway "lab")))))

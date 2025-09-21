(ns galt.payments.adapters.cln-payment-gateway-test
  (:require
    [clojure.test :refer [deftest testing is]]
    [failjure.core :as f]
    [galt.payments.domain.payment-gateway :refer [PaymentGateway] :as pg]
    [galt.payments.adapters.cln-payment-gateway :as cln-gw :refer [post-to-cln-node]]
    [galt.payments.adapters.cln-gateway-responses :as gw-responses]
    [galt.payments.adapters.cln-gateway-http-responses :as http-responses]
    [org.httpkit.fake :refer [with-fake-http]]))


(deftest cln-payment-gateway-test
  (testing "post-to-cln-node"
    (is (f/failed? (post-to-cln-node "http://localhost:1234" "fake-rune" :listinvoices)))

    (with-fake-http ["http://localhost:1234/v1/listinvoices" http-responses/invoices]
      (is (= 10123 (get-in
                     (post-to-cln-node "http://localhost:1234" "fake-rune" :listinvoices)
                     [:invoices 0 :amount-received-msat])))))

  (testing ":invoices"
    (let [http-post (constantly (:invoice-raw gw-responses/responses))
          gw (cln-gw/new-cln-payment-gateway http-post)
          response (pg/create-invoice gw {:amount-msat 10000 :label "Hello" :description "Wut"})]
      (is (instance? java.time.Instant (:expires-at response))))))

(ns galt.payments.adapters.btcpay-webhook-test
  (:require
    [buddy.core.codecs :as codecs]
    [buddy.core.mac :as mac]
    [clojure.test :refer [deftest is]]
    [galt.core.infrastructure.web.helpers :refer [->json]]
    [galt.payments.adapters.handlers :as handlers]))

(def secret "test-secret")

(defn signed [body-str]
  (let [raw (.getBytes body-str "UTF-8")
        hex (str (codecs/bytes->hex (mac/hash raw {:key secret :alg :hmac+sha256})))]
    {:raw raw :header (str "sha256=" hex)}))

(deftest valid-signature-test
  (let [{:keys [raw header]} (signed "{\"type\":\"InvoiceSettled\"}")]
    (is (handlers/valid-signature? secret raw header))
    (is (not (handlers/valid-signature? secret raw "sha256=00")))
    (is (not (handlers/valid-signature? secret raw nil)))
    (is (not (handlers/valid-signature? nil raw header)))))

(deftest handle-btcpay-webhook-test
  (let [{:keys [raw header]} (signed (->json {:type "InvoiceSettled"
                                              :metadata {:orderId "lab-1"}}))
        calls (atom [])
        deps {:btcpay-webhook-secret secret
              :update-invoice-use-case (fn [cmd] (swap! calls conj cmd) nil)}
        response (handlers/handle-btcpay-webhook deps raw header)]
    (is (= 200 (:status response)))
    (is (= [{:label "lab-1"}] @calls)))
  (let [{:keys [raw header]} (signed (->json {:type "InvoiceExpired"
                                              :metadata {:orderId "lab-1"}}))
        calls (atom [])
        response (handlers/handle-btcpay-webhook
                   {:btcpay-webhook-secret secret
                    :update-invoice-use-case (fn [cmd] (swap! calls conj cmd))}
                   raw header)]
    (is (= 200 (:status response)))
    (is (empty? @calls)))
  (let [response (handlers/handle-btcpay-webhook
                   {:btcpay-webhook-secret secret
                    :update-invoice-use-case (fn [_] (throw (ex-info "should not run" {})))}
                   (.getBytes "not-json" "UTF-8")
                   "sha256=dead")]
    (is (= 401 (:status response))))
  (let [{:keys [raw header]} (signed "not-json")
        response (handlers/handle-btcpay-webhook
                   {:btcpay-webhook-secret secret
                    :update-invoice-use-case (fn [_] nil)}
                   raw header)]
    (is (= 400 (:status response))))
  (let [{:keys [raw header]} (signed (->json {:type "InvoiceSettled"
                                              :metadata {:orderId "lab-1"}}))
        response (handlers/handle-btcpay-webhook
                   {:btcpay-webhook-secret secret
                    :update-invoice-use-case (fn [_] {:label "lab-1" :status "paid"})}
                   raw header)]
    (is (= 200 (:status response)))))

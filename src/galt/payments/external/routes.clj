(ns galt.payments.external.routes
  (:require
    [reitit.ring :as rr]
    [galt.payments.adapters.handlers :as handlers]))

(defn router
  [deps]
  (let [with-layout (:with-layout deps)
        with-deps-layout (partial with-layout deps)]
    (rr/router
      [["/payments/new" {:name :payments/new
                         :min-role :user
                         :handler (with-deps-layout handlers/new-payment)}]
       ["/payments/btcpay-webhook" {:handler (partial handlers/btcpay-webhook deps)}]])))

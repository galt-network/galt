(ns galt.payments.external.routes
  (:require
    [reitit.ring :as rr]
    [org.corfield.ring.middleware.data-json :refer [wrap-json-body]]
    [galt.payments.adapters.handlers :as handlers]))

(defn router
  [deps]
  (let [with-layout (:with-layout deps)
        with-deps-layout (partial with-layout deps)]
    (rr/router
      [["/payments/new" {:name :payments/new
                         :min-role :user
                         :handler (with-deps-layout handlers/new-payment)}]
       ;; To make core-lightning call back this endpoint, use the following plugin
       ;; https://github.com/fiatjaf/lightningd-webhook
       ["/payments/cln-webhook" {:handler (partial handlers/cln-webhook deps)
                                 :middleware [wrap-json-body]}]])))

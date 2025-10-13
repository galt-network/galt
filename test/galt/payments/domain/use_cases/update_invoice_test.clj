(ns galt.payments.domain.use-cases.update-invoice-test
  (:require
   [clojure.test :refer [deftest is]]
   [galt.payments.domain.use-cases.update-invoice :refer [update-invoice-use-case]]))

(deftest update-invoice-use-case-test
  (let [deps {:update-membership-invoice (fn [label params] {:status (:status params)})
              :ln-invoice-by-label (fn [label] {:status "paid"})
              :db-invoice-by-label (fn [label] {:label label})}
        label {:label "invoice-123"}
        result (update-invoice-use-case deps label)]
    (is (= (:status result) "paid"))))

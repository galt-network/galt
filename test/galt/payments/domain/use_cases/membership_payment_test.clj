(ns galt.payments.domain.use-cases.membership-payment-test
  (:require
   [clojure.test :refer [deftest is]]
   [galt.payments.domain.use-cases.membership-payment :refer [membership-payment-use-case]]))

(def bolt11 "lnbcrt10u1p5vjm5esp5zprjpy8f697439kwss220ezuk5d9c0vcq2mdzasw78gzl7xklazqpp5ldgqz3fdhv0ukvs9fqkurkjn0lefv07c7ldc70lrcgn9fn7ff6dsdpy2phkcctjypykuan0d93k2grxdaezqerpwejsxqyjw5qcqp29qxpqysgq90frf849yg3lypqc56hhrmqekhktuqwj6r6yr78dn4s45wyk54mrlqmdu06cr8k3lpjag2hggj962t4cn4ax606qe7s9jl8xmrfmecqpduhk33")

(def lightning-invoice-response
  {:payment-hash "hash"
   :expires-at 123
   :bolt-11 bolt11
   :payment-secret "paymentsecret..."
   :created-index 2})

(deftest membership-payment-use-case-test
  (let [user-id (random-uuid)
        deps {:membership-invoices (constantly [])
              :create-invoice (constantly lightning-invoice-response)
              :add-membership-invoice (fn [_user-id v] v)}
        [status invoice] (membership-payment-use-case deps {:user-id user-id})]
    (is (= :ok status))
    (is (= bolt11 (:bolt-11 invoice)))
    (is (= 123 (:expires-at invoice)))
    (is (= 960000 (:amount-msat invoice)))))

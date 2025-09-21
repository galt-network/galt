(ns galt.payments.domain.use-cases.membership-payment-test
  (:require
   [clojure.test :refer [deftest is]]
   [galt.payments.domain.use-cases.membership-payment :refer [membership-payment-use-case]]))

(def bolt11 "lnbcrt10u1p5vjm5esp5zprjpy8f697439kwss220ezuk5d9c0vcq2mdzasw78gzl7xklazqpp5ldgqz3fdhv0ukvs9fqkurkjn0lefv07c7ldc70lrcgn9fn7ff6dsdpy2phkcctjypykuan0d93k2grxdaezqerpwejsxqyjw5qcqp29qxpqysgq90frf849yg3lypqc56hhrmqekhktuqwj6r6yr78dn4s45wyk54mrlqmdu06cr8k3lpjag2hggj962t4cn4ax606qe7s9jl8xmrfmecqpduhk33")

(def lightning-invoice-response
  {:payment-hash "hash"
   :expires-at 123
   :bolt11 bolt11
   :payment-secret "paymentsecret..."
   :created-index 2})

(deftest membership-payment-use-case-test
  (let [invitation-id (random-uuid)
        user-id (random-uuid)
        invitation {:id invitation-id :max-usages 1}
        invitation-usage {:invitation-id invitation-id :user-id user-id :invoice-id nil}
        invoice {}
        deps {:invitation-by-id (constantly invitation)
              :invitation-usages-by-user (constantly invitation-usage)
              :request-lightning-invoice (constantly lightning-invoice-response)}
        command {:invitation-id (random-uuid) :user-id (random-uuid)}
        [status {:keys [invitation-usage invoice payment-status]}] (membership-payment-use-case deps command)]
    ; Mock lightning-node response, check for newly created invoice and its expired at
    (is (= (:bolt11 invoice) bolt11))))

(ns galt.payments.adapters.db-payment-repository
  (:require
    [galt.core.adapters.db-access :refer [query query-one]]
    [galt.core.adapters.db-result-transformations :refer [transform-row defaults ->local-date-time]]
    [galt.payments.domain.payment-repository :refer [PaymentRepository] :as pr]
    [galt.payments.domain.entities.membership-payment :refer [map->MembershipPayment]]))

(def invoice-spec
  {:invoices/id defaults
   :invoices/amount_msat defaults
   :invoices/label defaults
   :invoices/amount_received_msat defaults
   :invoices/status defaults
   :invoices/description defaults
   :invoices/bolt_11 defaults
   :invoices/payment_hash defaults
   :invoices/payment_secret defaults
   :invoices/payment_preimage defaults
   :invoices/created_index defaults
   :invoices/created_at [(first defaults) ->local-date-time]
   :invoices/expires_at [(first defaults) ->local-date-time]
   :invoices/paid_at [(first defaults) ->local-date-time]})

(defrecord DbPaymentRepository [db-access]
  PaymentRepository

  (add-membership-invoice [_ user-id params]
    (let [invoice-query {:insert-into [:invoices] :values [params] :returning [:*]}
          added-invoice (query-one db-access invoice-query)
          invoice-id (:invoices/id added-invoice)
          membership-payment-query {:insert-into [:galt-membership-payments]
                                    :values [{:user-id user-id :invoice-id invoice-id}]
                                    :returning [:*]}]
      ; TODO merge & return MembershipPayment to have both user-id & invoice data
      (query-one db-access membership-payment-query)
      (transform-row invoice-spec added-invoice)))

  (update-membership-invoice [_ id-or-label params]
    (->> {:update [:invoices]
          :set (update params :status (fn [status] [:cast status :invoicestatus]))
          :where (if (string? id-or-label) [:= :label id-or-label] [:= :id id-or-label])
          :returning [:*]}
         (query-one db-access ,,,)
         (transform-row invoice-spec ,,,)))

  (membership-invoices [_ user-id]
    (->> {:select [:invoices.*]
          :from [:invoices]
          :join [[:galt_membership_payments :gmp] [:= :gmp.invoice_id :invoices.id]]
          :where [:= :gmp.user_id user-id]
          :order-by [[:invoices.created_at :desc]]}
         (query db-access ,,,)
         (map #(transform-row invoice-spec %) ,,,)))

  (invoice-by-label [_ label]
    (some->>
      {:select [:*] :from [:invoices] :where [:= :label label]}
      (query-one db-access ,,,)
      (transform-row invoice-spec ,,,))))

(def last-repo (atom nil))
(defn new-db-payment-repository [db-access]
  (reset! last-repo (DbPaymentRepository. db-access))
  (DbPaymentRepository. db-access))

(comment
  (def user-id (parse-uuid "019954c1-bc96-706d-85b2-4b9b01e197c7"))
  (pr/membership-invoices @last-repo user-id)
  (require '[galt.payments.adapters.cln-gateway-responses :refer [responses]])
  (pr/add-membership-invoice @last-repo user-id (:invoice responses)))

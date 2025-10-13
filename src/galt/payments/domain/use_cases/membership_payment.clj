(ns galt.payments.domain.use-cases.membership-payment
  (:require
    [java-time.api :as jt]
    [galt.core.adapters.time-helpers :as th]
    [failjure.core :as f]
    ))

(defn membership-invoice-params
  [user-id]
  {:amount-msat (* 960 1000)
   :label (str "membership-payment-" user-id "-" (th/timestamp-id))
   :description (str "Galt membership payment for " user-id)})

(defn latest-non-expired-invoice [invoices]
  (let [not-expired? (fn [invoice] (jt/> (:expires-at invoice) (jt/local-date-time)))]
    (first (filter not-expired? invoices))))

(def last-deps (atom nil))

(defn membership-payment-use-case
  "Facilitates the membership payment by providing existing non-expired invoice
  or requesting a new one from the Lightning node.
  Once the invoice is paid, another use case creates the member"
  [{:keys [membership-invoices create-invoice add-membership-invoice] :as deps}
   {:keys [user-id] :as command}]
  (reset! last-deps deps)
  (let [existing-invoice (latest-non-expired-invoice (membership-invoices user-id))
        invoice-params (membership-invoice-params user-id)
        invoice (if existing-invoice
                  existing-invoice
                  (f/as-ok-> (create-invoice invoice-params) v
                    (select-keys v [:bolt-11 :expires-at :payment-hash :payment-secret :created-index])
                    (merge v invoice-params)
                    (add-membership-invoice user-id v)))]
    (if (f/failed? invoice)
      [:error {:message (:message invoice)}]
      [:ok invoice])))

(comment
  (require '[galt.main :refer [running-system]])
  (def uc (get-in @running-system [:donut.system/instances :use-cases :membership-payment-use-case]))
  (uc {:user-id (parse-uuid "019954c1-bc96-706d-85b2-4b9b01e197c7")})
  (select-keys ((:create-invoice @last-deps) (membership-invoice-params (parse-uuid "019954c1-bc96-706d-85b2-4b9b01e197c7"))) [:bolt-11 :expires-at])
  )

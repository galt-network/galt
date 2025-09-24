(ns galt.payments.domain.payment-repository)

(defprotocol PaymentRepository
  (add-membership-invoice [this user-id invoice])
  (update-membership-invoice [this id-or-label params]
                             "id-or-label used as id when integer, as label when string")
  (invoice-by-label [this label])
  (membership-invoices [this user-id])
  (current-membership-payment [this user-id])
  )

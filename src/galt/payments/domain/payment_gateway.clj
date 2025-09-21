(ns galt.payments.domain.payment-gateway)

(defprotocol PaymentGateway
  (create-invoice [this {:keys [amount-msat label description expiry]}])
  (list-invoices [this])
  (invoice-by-hash [this payment-hash])
  (invoice-by-label [this label])
  (call-method [this method params] [this method]))

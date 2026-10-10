(ns galt.payments.domain.payment-gateway)

(defprotocol PaymentGateway
  (create-invoice [this {:keys [amount-msat label description expiry]}])
  (invoice-by-label [this label]))

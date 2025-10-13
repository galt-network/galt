(ns galt.payments.domain.use-cases.update-invoice)

(defn update-invoice-use-case
  [{:keys [update-membership-invoice
           ln-invoice-by-label
           db-invoice-by-label]}
   {:keys [label]}]
  ; TODO need to check first that the invoice label is in the database
  ;      because the lightning node may be calling back for other invoices too
  (let [existing-invoice (db-invoice-by-label label)
        updated-params (ln-invoice-by-label label)]
    (when existing-invoice (update-membership-invoice label (merge existing-invoice updated-params)))))

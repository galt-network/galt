(ns galt.payments.domain.entities.membership-payment)

(defrecord MembershipPayment [user-id
                              invoice-id
                              amount-msat
                              label
                              status
                              description
                              bolt-11
                              created-at
                              paid-at])

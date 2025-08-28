(ns galt.core.adapters.db-access)

(defprotocol DbAccess
  (query [this query-vec])
  (query-one [this query-vec])
  (in-transaction [this callback] "Calls (callback query-fn) within a transaction"))

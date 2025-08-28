(ns galt.core.adapters.postgres-db-access
  (:require
    [galt.core.adapters.db-access :as db-access :refer [DbAccess]]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]))

(defn query [conn query-vec]
  (println ">>> postgres-db-access/query" (sql/format query-vec))
  (->> query-vec
       (sql/format ,,,)
       (jdbc/execute! conn ,,,)))

(defrecord PostgresDbAccess [conn]
  DbAccess

  (query [_ query-vec]
    (query conn query-vec))

  (query-one [_ query-vec]
    (first (query conn query-vec)))

  (in-transaction [_ callback]
    (jdbc/with-transaction [tx conn]
      (callback (partial query tx)))))

(defn new-db-access [conn]
  (PostgresDbAccess. conn))

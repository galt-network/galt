(ns galt.core.infrastructure.web.db-session-store
  (:require
    [galt.core.adapters.db-access :refer [query-one]]
    [ring.middleware.session.store :refer [SessionStore]]))

(defrecord DbSessionStore [db-access]
  SessionStore

  (read-session [_ key]
    (query-one db-access {:select [:*] :from [:session_storage] :where [:= :id key]}))

  (write-session [_ key data]
    (query-one db-access {:insert-into [:session_storage] :values [{:id key :data data}]}))
  (delete-session [_ key]))

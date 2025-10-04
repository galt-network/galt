(ns galt.core.infrastructure.db-migrations
  (:require
    [next.jdbc :as jdbc]
    [migratus.core :as migratus]))

(def database-name (System/getenv "MIGRATUS_DATABASE"))
(def database-user (System/getenv "MIGRATUS_USER"))
(def database-pass (System/getenv "MIGRATUS_PASSWORD"))

(defn datasource-config
  ([] (datasource-config database-name))
  ([db-name]
    {:jdbcUrl (str "jdbc:postgresql://localhost:5432/" db-name)
     :user database-user
     :password database-pass}))

(defn get-datasource
  [config]
  (jdbc/get-datasource config))

(def config {:store                :database
             :migration-dir        "migrations/"
             :init-script          "init.sql" ;script should be located in the :migration-dir path
             :init-in-transaction? false
             :properties {}
             :db {:datasource (get-datasource (datasource-config))}})

(def init-config (assoc-in config [:db :datasource] (get-datasource (datasource-config "postgres"))))

(defn init! [] (migratus/init init-config))
(defn migrate! [] (migratus/migrate config))

(comment
  (migratus/init init-config)

  ; create new migration files (.up & .down)
  (migratus/create config "create-comments")

  (migratus/pending-list config)
  ;apply pending migrations
  (migratus/migrate config)

  ;rollback the migration with the latest timestamp
  (migratus/rollback config)

  ;bring up migrations matching the ids
  (migratus/up config 20250830152849)
  (migratus/up config 20250915223524)

  ;bring down migrations matching the ids
  (migratus/down config 20250830152849)
  (migratus/down config 20250915223524)
  )

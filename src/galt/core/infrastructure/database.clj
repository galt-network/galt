(ns galt.core.infrastructure.database
   (:require
     [next.jdbc :as jdbc]
     [honey.sql :as sql])
   (:import
     [com.zaxxer.hikari HikariConfig HikariDataSource]))


(defn make-config
  [config]
  {:jdbcUrl (str "jdbc:postgresql://localhost:5432/" (get config :database "galt"))
   :user (get config :user "postgres")
   :password (get config :password "postgres")
   :maximumPoolSize (get config :maximumPoolSize 4)
   :connectionTimeout (get config :connectionTimeout 12000)})

; Another way to create pooled datasource with next.jdbc directly:
;  https://cljdoc.org/d/com.github.seancorfield/next.jdbc/1.3.1048/doc/getting-started#connection-pooling
(defn create-pooled-datasource [db-spec]
  (let [config (doto (HikariConfig.)
                 (.setJdbcUrl (:jdbcUrl db-spec))
                 (.setUsername (:user db-spec))
                 (.setPassword (:password db-spec))
                 (.setMaximumPoolSize 3)
                 (.setConnectionTimeout 10000))]
    (HikariDataSource. config)))

(defn get-all-users [ds]
  (jdbc/execute! ds (sql/format {:select [:*] :from [:users]})))

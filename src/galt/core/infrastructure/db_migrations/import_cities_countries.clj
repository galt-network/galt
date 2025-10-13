(ns galt.core.infrastructure.db-migrations.import-cities-countries
  (:require
    [clojure.java.io :as io]
    [clojure.java.shell :as sh]
    [next.jdbc :as jdbc]))

(defn count-lines [filepath]
  (with-open [rdr (io/reader filepath)]
    (count (line-seq rdr))))

(defn migrate-up
  [{:keys [db conn] :as config} sql-path-env-var]
  (println "import-cities-countries UP" [config sql-path-env-var])
  (let [world-path (System/getenv sql-path-env-var)
        world-file (io/file world-path)]
    (when (not (.exists world-file))
      (throw (ex-info "world.sql file not found" {:path world-path})))
    (println (str "Importing "
                  world-path
                  " Might take some time. Lines of SQL to process: " (count-lines world-path)))
    ; (jdbc/execute! db [(slurp world-path)])
    (let [db-user (System/getenv "MIGRATUS_USER")
          db-password (System/getenv "MIGRATUS_PASSWORD")
          db-name (System/getenv "MIGRATUS_DATABASE")
          db-url (str "postgresql://" db-user ":" db-password "@localhost:5432/" db-name)
          result (sh/sh "psql" "-d" db-url "-f" world-path)]
      (if (not (zero? (:exit result)))
        (throw (ex-info "Error importing world geodata" result))
        (println "Done importing " world-path))))
  [config sql-path-env-var])

(defn migrate-down
  [{:keys [db]}]
  (println "import-cities-countries DOWN")
  (let [tables-to-drop ["cities" "countries" "states" "regions" "subregions"]]
    (dorun
      (map (fn [table]
             (println "Dropping table --> " table)
             (jdbc/execute! db [(str "DROP TABLE IF EXISTS " table " CASCADE;")]))
           tables-to-drop))
    (println "Done dropping tables: " tables-to-drop)))

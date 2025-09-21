(ns galt.core.infrastructure.db-migrations.import-cities-countries
  (:require
    [clojure.java.io :as io]
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
    (jdbc/execute! db [(slurp world-path)])
    (println "Done importing " world-path)
    )
  [config sql-path-env-var])

(defn migrate-down
  [{:keys [db]}]
  (let [tables-to-drop ["cities" "countries" "states" "regions" "subregions"]]
    (dorun
      (map (fn [table]
             (println "Dropping table:" table)
             (jdbc/execute! db [(str "DROP TABLE IF EXISTS " table " CASCADE")]))
           tables-to-drop))
    (println "Done dropping tables: " tables-to-drop)))

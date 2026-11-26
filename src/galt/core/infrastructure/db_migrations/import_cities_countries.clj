(ns galt.core.infrastructure.db-migrations.import-cities-countries
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [next.jdbc :as jdbc]))

(def sample "geodata/cities-countries-sample.sql")

(defn psql-only?
  [line]
  (boolean (re-matches #"\\(?:un)?restrict.*|ALTER TABLE \S+ OWNER TO \S+;\s*" line)))

(defn- statements
  "Groups lines into complete statements; pg_dump ends every statement with `;`."
  [lines]
  (lazy-seq
    (when (seq lines)
      (let [[body end] (split-with #(not (str/ends-with? % ";")) lines)]
        (cons (concat body (take 1 end)) (statements (rest end)))))))

(defn statement-chunks
  "Groups SQL lines into lazy sequences of `n` statements."
  [n lines]
  (->> (statements lines)
       (partition-all n)
       (map #(apply concat %))))

(defn import-dump!
  "Imports a pg_dump file in bounded memory. Runs on its own connection: the dump
  clears the session search_path, which would break the caller's connection."
  [db source]
  (with-open [conn (jdbc/get-connection db)
              lines (io/reader source)]
    (doseq [chunk (->> (line-seq lines)
                       (remove psql-only?)
                       (statement-chunks 5000))]
      (jdbc/execute! conn [(str/join "\n" chunk)]))))

(defn migrate-up
  [{:keys [db]} sql-path-env-var]
  (let [source (or (System/getenv sql-path-env-var)
                   (io/resource sample)
                   (throw (ex-info "geodata sample not found on the classpath" {})))]
    (println "import-cities-countries UP" (str source))
    (import-dump! db source)))

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

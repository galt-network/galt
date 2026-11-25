(ns galt.core.infrastructure.db-migrations-test
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [clojure.test :refer [deftest is]]
    [galt.core.infrastructure.db-migrations :as db-migrations]
    [migratus.properties :as props]))

(deftest init-script-test
  (let [script (props/inject-properties (props/load-properties db-migrations/config)
                                        (slurp (io/resource "migrations/init.sql")))]
    (is (str/includes? script (str "CREATE DATABASE " db-migrations/database-name
                                   " WITH OWNER " db-migrations/database-user ";")))
    (is (not (str/includes? script "${")))))

(ns galt.core.infrastructure.version
  (:require
   [clojure.edn :as edn]
   [clojure.java.io]
   [clojure.java.shell :refer [sh]])
  (:import
   [java.net URL]))

(defn running-from-jar? []
  (let [resource (clojure.java.io/resource "galt/main.clj")]
    (if resource
      (= "jar" (.getProtocol ^URL resource))
      false)))

(defn get-commit-hash
  []
  (:out (sh "git" "rev-parse" "--short" "HEAD")))

(defn- read-commit-hash-from-file []
  (try
    (let [resource (clojure.java.io/resource "build-info.edn") ; Written in build.clj
          build-info (when resource (edn/read-string (slurp resource)))]
      (:git-commit-hash build-info))
    (catch Exception _e
      "unknown")))

(defn commit-hash
  []
  (if (running-from-jar?)
    (read-commit-hash-from-file)
    (get-commit-hash)))

(comment
  (println "JAR?" (running-from-jar?)))

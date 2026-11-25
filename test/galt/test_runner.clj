(ns galt.test-runner
  "`clojure -M:test [namespace ...]`: with no arguments, every `*_test.clj` under `test/`."
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [clojure.test :as t]))

(defn- test-namespaces
  [dir]
  (for [f (file-seq (io/file dir))
        :when (str/ends-with? (str f) "_test.clj")
        :let [rel (subs (str f) (inc (count (str dir))))]]
    (-> rel
        (str/replace #"\.clj$" "")
        (str/replace "/" ".")
        (str/replace "_" "-")
        symbol)))

(defn -main
  [& args]
  (let [namespaces (or (seq (map symbol args)) (test-namespaces "test"))
        summary (do (apply require namespaces)
                    (apply t/run-tests namespaces))]
    (System/exit (if (pos? (+ (:fail summary 0) (:error summary 0))) 1 0))))

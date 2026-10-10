(ns galt.core.system-test
  (:require
    [clojure.java.io :as io]
    [clojure.test :refer [deftest is]]
    [galt.core.system :as system]))

(deftest read-dotenv-test
  (let [path (str (System/getProperty "java.io.tmpdir") "/galt-dotenv-" (System/nanoTime))]
    (spit path "\n# comment\nFOO=bar\nBAZ=a=b\nnot a line\n")
    (try
      (is (= {"FOO" "bar" "BAZ" "a=b"} (system/read-dotenv path)))
      (is (= {} (system/read-dotenv (str path "-missing"))))
      (finally
        (io/delete-file path true)))))

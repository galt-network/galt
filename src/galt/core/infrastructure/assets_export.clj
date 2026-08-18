(ns galt.core.infrastructure.assets-export
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [galt.core.domain.asset-store :refer [get-object list-objects]]
    [galt.core.infrastructure.asset-stores :as asset-stores]))

(defn- write-object [out-dir key in-stream]
  (let [target (io/file out-dir key)]
    (io/make-parents target)
    (with-open [in in-stream
                out (io/output-stream target)]
      (io/copy in out))
    (.length target)))

(defn export-public! [config out-dir]
  (let [file-storage (asset-stores/new-file-storage config)
        store (:store file-storage)
        keys (list-objects store "public/")]
    (when (seq keys)
      (io/make-parents (io/file out-dir)))
    (reduce (fn [acc key]
              (let [bytes-written (write-object out-dir key (get-object store key))]
                {:files (inc (:files acc))
                 :bytes (+ (:bytes acc) bytes-written)}))
            {:files 0 :bytes 0}
            keys)))

(ns galt.core.infrastructure.disk-asset-store
  "Disk implementation of the AssetStore protocol. Dev/test default."
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [ring.util.response]
    [galt.core.domain.asset-store :refer [AssetStore put-object get-object
                                          get-content-response delete-object
                                          presign-url list-objects]])
  (:import
    [java.io File FileInputStream]
    [java.nio.file Files Path]))

(defrecord DiskAssetStore [storage-root root-url]
  AssetStore
  (put-object [_ {:keys [key content-type size input-stream]}]
    (let [target (io/file storage-root key)]
      (io/make-parents target)
      (with-open [in input-stream]
        (io/copy in target))
      {:key key :content-type content-type :size size}))
  (get-object [_ key]
    (let [f (io/file storage-root key)]
      (when (.isFile f)
        (FileInputStream. f))))
  (get-content-response [_ key]
    (ring.util.response/file-response key {:root storage-root}))
  (delete-object [_ key]
    (Files/deleteIfExists (.toPath (io/file storage-root key))))
  (presign-url [_ key expires-in-seconds]
    (str root-url "/files/" key))
  (list-objects [_ prefix]
    (let [root (.toPath (io/file storage-root))
          prefix (or prefix "")]
      (when (Files/isDirectory root (make-array java.nio.file.LinkOption 0))
        (->> (Files/walk root (make-array java.nio.file.FileVisitOption 0))
             (.iterator)
             (iterator-seq)
             (filter #(Files/isRegularFile % (make-array java.nio.file.LinkOption 0)))
             (map #(subs (str %) (inc (count (str root)))))
             (filter #(str/starts-with? % prefix)))))))

(defn new-disk-asset-store
  [config]
  (->DiskAssetStore
    (or (:storage-root config) "resources/uploads")
    (or (:root-url config) "http://localhost:8081")))

(ns galt.core.infrastructure.disk-file-storage
  (:require
    [ring.util.response]
    [clojure.java.io :as io])
  (:import
    [java.io File]
    [java.nio.file Files StandardCopyOption]))

(defn content-response
  [config file-path]
  (ring.util.response/file-response file-path {:root (:storage-root config)}))

(defn store-content
  [config {:keys [filename ^File tempfile]}]
  (let [storage-root (:storage-root config)
        root-url (:root-url config)
        file-id (str (random-uuid))
        original-file-name filename
        temp-file-path (.toPath tempfile)
        stored-file-name (str file-id "__" original-file-name)
        target-path (.toPath (io/file storage-root stored-file-name))
        stored-url (str root-url "/files/" stored-file-name)]
    (Files/move temp-file-path target-path (into-array [StandardCopyOption/REPLACE_EXISTING]))
    {:id file-id :name stored-file-name :url stored-url}))

(ns galt.core.domain.use-cases.upload-asset
  "Upload hardening use case: validates file type/size, computes sha256,
  dedups against existing assets, stores the file and records asset metadata."
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [galt.core.domain.asset-repository :refer [find-by-sha256-visibility add-asset]]
    [galt.core.domain.asset-store :refer [put-object]]))

(def content-type->ext
  {"image/jpeg" "jpg"
   "image/png" "png"
   "image/webp" "webp"
   "image/avif" "avif"
   "image/gif" "gif"
   "video/mp4" "mp4"
   "video/webm" "webm"
   "model/gltf-binary" "glb"
   "model/gltf+json" "gltf"
   "application/zip" "zip"})

(defn category-of [content-type]
  (cond
    (str/starts-with? content-type "image/") :image
    (str/starts-with? content-type "video/") :video
    (str/starts-with? content-type "model/") :model
    (= content-type "application/zip") :archive))

(def max-bytes-key
  {:image :image-max-bytes
   :video :video-max-bytes
   :model :model-max-bytes
   :archive :archive-max-bytes})

(defn sha256-hex [file]
  (let [digest (java.security.MessageDigest/getInstance "SHA-256")]
    (with-open [in (io/input-stream file)]
      (let [buf (byte-array 65536)]
        (loop []
          (let [n (.read in buf)]
            (when (pos? n)
              (.update digest buf 0 n)
              (recur))))))
    (apply str (map #(format "%02x" (bit-and % 0xff)) (.digest digest)))))

(defn- file-size [{:keys [size tempfile]}]
  (or (when (and size (pos? (long size))) (long size))
      (when tempfile (.length ^java.io.File tempfile))))

(defn upload-asset-use-case
  "deps: {:asset-store AssetStore :asset-repo AssetRepository
          :upload-config {:image-max-bytes ... :video-max-bytes ...
                          :model-max-bytes ... :archive-max-bytes ...}
          :public-prefix \"public\" :private-prefix \"private\"}
  command: {:file {:filename :content-type :size :tempfile}
            :visibility :public | :private
            :owner-member-id uuid | nil :module string | nil}
  Returns [:ok {:key ... :deduplicated? ... :asset ...} nil] or
  [:error nil [error-messages]]."
  [{:keys [asset-store asset-repo upload-config public-prefix private-prefix]} command]
  (let [{:keys [file visibility owner-member-id module]} command
        {:keys [filename content-type]} file
        size (file-size file)
        max-bytes (when (contains? content-type->ext content-type)
                    (get upload-config (max-bytes-key (category-of content-type))))
        validation-errors
        (cond-> []
          (nil? file) (conj "No file provided")
          (or (nil? content-type) (not (contains? content-type->ext content-type)))
          (conj (str "Unsupported file type" (when content-type (str ": " content-type))))
          (and size max-bytes (> size max-bytes))
          (conj (str "File too large (max " (quot max-bytes 1048576) " MB)"))
          (not (contains? #{:public :private} visibility))
          (conj "Invalid visibility"))]
    (if (seq validation-errors)
      [:error nil validation-errors]
      (let [sha (sha256-hex (:tempfile file))
            ext (get content-type->ext content-type)
            prefix (if (= :private visibility) private-prefix public-prefix)
            key (str prefix "/" sha "." ext)
            existing (find-by-sha256-visibility asset-repo sha (name visibility))]
        (if existing
          [:ok {:key (:storage-key existing) :deduplicated? true :asset existing} nil]
          (do
            (put-object asset-store {:key key
                                     :content-type content-type
                                     :size size
                                     :input-stream (io/input-stream (:tempfile file))
                                     :filename filename})
            (let [asset (add-asset asset-repo
                                   {:sha256 sha
                                    :content-type content-type
                                    :size size
                                    :original-filename filename
                                    :storage-key key
                                    :visibility (name visibility)
                                    :owner-member-id owner-member-id
                                    :module module})]
              [:ok {:key key :deduplicated? false :asset asset} nil])))))))

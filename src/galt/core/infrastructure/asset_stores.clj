(ns galt.core.infrastructure.asset-stores
  "Facade over AssetStore implementations. Builds a disk or S3-compatible store
  from config and exposes URL/key helpers used across handlers and view models."
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [galt.core.domain.asset-store :refer [put-object get-object
                                          get-content-response delete-object
                                          presign-url list-objects]]
    [galt.core.infrastructure.disk-asset-store :as disk]
    [galt.core.infrastructure.s3-asset-store :as s3]))

(defn- normalize-config
  "Fill in defaults and backward-compat. Accepts either a full config map
  (with nested :asset-store), an already-normalized asset-store map, or a
  legacy config (:file-storage-root / :galt-root-url keys)."
  [config]
  (let [c (or (:asset-store config)
              (not-empty
               (select-keys config [:type :storage-root :root-url :bucket :endpoint
                                    :region :access-key :secret-key :public-base-url
                                    :public-prefix :private-prefix :upload-config]))
              {:type :disk
               :storage-root (:file-storage-root config)
               :root-url (:galt-root-url config)})
        merged (merge {:type :disk
                       :storage-root "resources/uploads"
                       :root-url "http://localhost:8081"
                       :region "auto"
                       :public-prefix "public"
                       :private-prefix "private"}
                      (into {} (remove (comp nil? val)) c))]
    (if (:public-base-url merged)
      merged
      (assoc merged :public-base-url (:root-url merged)))))

(defn- public-key? [config key]
  (or (str/starts-with? key (str (:public-prefix config) "/"))
      (not (str/starts-with? key (str (:private-prefix config) "/")))))

(defn asset-url
  "Compose the render-time URL for a stored key. Passes through absolute URLs
  (http/https) and app-relative paths (/assets/...) untouched. A nil key
  returns nil. opts (optional) appended as a query string (e.g. CF Images
  resize params)."
  [config key & [opts]]
  (when key
    (let [query (when (seq opts)
                  (str "?" (str/join "&" (map (fn [[k v]] (str (name k) "=" v)) opts))))]
      (cond
        (or (str/starts-with? key "http://") (str/starts-with? key "https://")
            (str/starts-with? key "/"))
        (str key query)

        (= :disk (:type (normalize-config config)))
        (str (:root-url (normalize-config config)) "/files/" key query)

        (public-key? (normalize-config config) key)
        (str (:public-base-url (normalize-config config)) "/" key query)

        :else
        (str (:root-url (normalize-config config)) "/private-assets/" key query)))))

(defn asset-key
  "Strip the known store URL prefixes from a value to get back the storage key.
  Returns nil for nil/empty. Non-store URLs pass through unchanged (seeds,
  external avatars)."
  [config value]
  (when (and value (not (str/blank? value)))
    (let [c (normalize-config config)
          root-url (:root-url c)
          public-base-url (or (:public-base-url c) root-url)]
      (cond
        (str/starts-with? value (str root-url "/files/"))
        (subs value (count (str root-url "/files/")))

        (str/starts-with? value (str public-base-url "/"))
        (subs value (count (str public-base-url "/")))

        :else value))))

(defn new-file-storage
  "Build the file-storage facade map consumed by the system :file-storage
  component. Keeps the legacy :store-content / :content-response keys so
  existing consumers keep working."
  [config]
  (let [c (normalize-config config)
        store (case (:type c)
                :s3 (s3/new-s3-asset-store c)
                :disk (disk/new-disk-asset-store c))]
    {:store store
     :store-content
     (fn [{:keys [filename content-type size tempfile] :as _file}]
       (let [key (str (random-uuid) "__" filename)]
         (put-object store {:key key
                            :content-type content-type
                            :size size
                            :input-stream (io/input-stream tempfile)
                            :filename filename})
         {:id (subs key 0 (str/index-of key "__"))
          :name key
          :url (asset-url c key)}))
     :content-response (partial get-content-response store)
     :public-prefix (:public-prefix c)
     :private-prefix (:private-prefix c)
     :asset-url (partial asset-url c)
     :asset-key (partial asset-key c)
     :get-object (partial get-object store)
     :delete-object (partial delete-object store)
     :presign-url (partial presign-url store)
     :list-objects (partial list-objects store)}))

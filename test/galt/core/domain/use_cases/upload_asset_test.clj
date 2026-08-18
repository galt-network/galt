(ns galt.core.domain.use-cases.upload-asset-test
  (:require
    [clojure.java.io :as io]
    [clojure.test :refer [deftest is testing]]
    [galt.core.domain.asset-repository :refer [AssetRepository add-asset
                                               find-by-sha256-visibility
                                               find-asset-by-id find-by-storage-key
                                               delete-asset list-assets]]
    [galt.core.domain.asset-store :refer [AssetStore put-object get-object
                                          get-content-response delete-object
                                          presign-url list-objects]]
    [galt.core.domain.use-cases.upload-asset :as ua]))

(defrecord FakeStore [puts]
  AssetStore
  (put-object [_ asset] (swap! puts conj asset) {:key (:key asset)})
  (get-object [_ key] nil)
  (get-content-response [_ key] nil)
  (delete-object [_ key] nil)
  (presign-url [_ key expires-in-seconds] nil)
  (list-objects [_ prefix] nil))

(defrecord FakeRepo [assets added]
  AssetRepository
  (add-asset [_ asset]
    (let [a (assoc asset :id (random-uuid) :created-at (java.time.LocalDateTime/now))]
      (swap! added conj a)
      a))
  (find-asset-by-id [_ id] nil)
  (find-by-sha256-visibility [_ sha256 visibility] (get @assets [sha256 visibility]))
  (find-by-storage-key [_ storage-key] nil)
  (delete-asset [_ id] nil)
  (list-assets [_] nil))

(defn- temp-file [bytes]
  (let [f (io/file (str (System/getProperty "java.io.tmpdir") "/upload-test-" (random-uuid)))]
    (io/copy bytes f)
    f))

(defn- test-deps []
  {:asset-store (FakeStore. (atom []))
   :asset-repo (FakeRepo. (atom {}) (atom []))
   :upload-config {:image-max-bytes 10485760
                   :video-max-bytes 104857600
                   :model-max-bytes 104857600
                   :archive-max-bytes 104857600}
   :public-prefix "public"
   :private-prefix "private"})

(defn- upload [deps file]
  (ua/upload-asset-use-case deps
                            {:file file
                             :visibility (:visibility file :public)}))

(deftest happy-path-stores-and-returns-key
  (let [deps (test-deps)
        f (temp-file (.getBytes "hello"))
        [status result errors] (upload deps {:filename "photo.png"
                                             :content-type "image/png"
                                             :size 5
                                             :tempfile f})]
    (is (= :ok status))
    (is (nil? errors))
    (is (= "public/2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824.png"
           (:key result)))
    (is (false? (:deduplicated? result)))
    (let [put (first @(:puts (:asset-store deps)))]
      (is (= "public/2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824.png"
             (:key put)))
      (is (= "image/png" (:content-type put))))
    (is (= 1 (count @(:added (:asset-repo deps)))))
    (is (= "public/2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824.png"
           (:storage-key (first @(:added (:asset-repo deps))))))))

(deftest unsupported-content-type-is-rejected
  (let [deps (test-deps)
        f (temp-file (.getBytes "hello"))
        [status result errors] (upload deps {:filename "notes.txt"
                                             :content-type "text/plain"
                                             :size 5
                                             :tempfile f})]
    (is (= :error status))
    (is (nil? result))
    (is (= ["Unsupported file type: text/plain"] errors))
    (is (empty? @(:puts (:asset-store deps))))
    (is (empty? @(:added (:asset-repo deps))))))

(deftest oversize-file-is-rejected
  (let [deps (test-deps)
        f (temp-file (byte-array (* 11 1024 1024) (byte 1)))
        [status result errors] (upload deps {:filename "big.png"
                                             :content-type "image/png"
                                             :size (* 11 1024 1024)
                                             :tempfile f})]
    (is (= :error status))
    (is (= ["File too large (max 10 MB)"] errors))
    (is (empty? @(:puts (:asset-store deps))))))

(deftest duplicate-upload-dedups-without-storing
  (let [deps (test-deps)
        f (temp-file (.getBytes "hello"))
        existing {:id (random-uuid)
                  :sha256 "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"
                  :storage-key "public/2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824.png"
                  :visibility "public"}
        _ (swap! (:assets (:asset-repo deps)) assoc
                 ["2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824" "public"]
                 existing)
        [status result errors] (upload deps {:filename "photo.png"
                                             :content-type "image/png"
                                             :size 5
                                             :tempfile f})]
    (is (= :ok status))
    (is (nil? errors))
    (is (true? (:deduplicated? result)))
    (is (= (:storage-key existing) (:key result)))
    (is (empty? @(:puts (:asset-store deps))))
    (is (empty? @(:added (:asset-repo deps))))))

(deftest private-visibility-uses-private-prefix
  (let [deps (test-deps)
        f (temp-file (.getBytes "hello"))
        [status result errors] (upload deps {:filename "clip.mp4"
                                             :content-type "video/mp4"
                                             :size 5
                                             :tempfile f
                                             :visibility :private})]
    (is (= :ok status))
    (is (nil? errors))
    (is (= "private/2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824.mp4"
           (:key result)))
    (is (= "private" (:visibility (first @(:added (:asset-repo deps))))))))

(deftest missing-file-is-rejected
  (let [deps (test-deps)
        [status result errors] (upload deps nil)]
    (is (= :error status))
    (is (= ["No file provided" "Unsupported file type"] errors))))

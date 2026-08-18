(ns galt.core.infrastructure.disk-asset-store-test
  (:require
    [clojure.java.io :as io]
    [clojure.test :refer [deftest is testing]]
    [galt.core.domain.asset-store :refer [put-object get-object
                                          get-content-response delete-object
                                          presign-url list-objects]]
    [galt.core.infrastructure.disk-asset-store :as store]))

(defn bytes->stream [bs]
  (java.io.ByteArrayInputStream. bs))

(defn temp-dir []
  (let [d (io/file (System/getProperty "java.io.tmpdir") (str "galt-assets-" (random-uuid)))]
    (.mkdirs d)
    d))

(defn stream->bytes [resp]
  (with-open [in (io/input-stream (:body resp))]
    (let [baos (java.io.ByteArrayOutputStream.)]
      (io/copy in baos)
      (.toByteArray baos))))

(deftest put-and-get-roundtrip
  (let [root (temp-dir)]
    (try
      (let [s (store/new-disk-asset-store {:storage-root (.getPath root) :root-url "http://x"})
            asset {:key "public/abc.png" :content-type "image/png" :size 3
                   :input-stream (bytes->stream (.getBytes "abc"))}]
        (is (= {:key "public/abc.png" :content-type "image/png" :size 3}
               (put-object s asset)))
        (is (= (vec (.getBytes "abc"))
               (vec (stream->bytes (get-content-response s "public/abc.png")))))
        (testing "get-object returns an InputStream"
          (with-open [in (get-object s "public/abc.png")]
            (is (= "abc" (slurp in))))))
      (finally
        (io/delete-file root true)))))

(deftest put-nested-key-creates-parents
  (let [root (temp-dir)]
    (try
      (let [s (store/new-disk-asset-store {:storage-root (.getPath root)})]
        (put-object s {:key "private/videos/x.mp4" :content-type "video/mp4"
                             :input-stream (bytes->stream (.getBytes "vid"))})
        (is (.exists (io/file root "private/videos/x.mp4"))))
      (finally
        (io/delete-file root true)))))

(deftest delete-object-removes-file
  (let [root (temp-dir)]
    (try
      (let [s (store/new-disk-asset-store {:storage-root (.getPath root)})]
        (put-object s {:key "public/abc.png" :input-stream (bytes->stream (.getBytes "x"))})
        (is (true? (delete-object s "public/abc.png")))
        (is (nil? (get-object s "public/abc.png"))))
      (finally
        (io/delete-file root true)))))

(deftest presign-url-shape
  (let [s (store/new-disk-asset-store {:storage-root "r" :root-url "http://localhost:8081"})]
    (is (= "http://localhost:8081/files/public/abc.png"
           (presign-url s "public/abc.png" 3600)))))

(deftest list-objects-filters-by-prefix
  (let [root (temp-dir)]
    (try
      (let [s (store/new-disk-asset-store {:storage-root (.getPath root)})]
        (put-object s {:key "public/a.png" :input-stream (bytes->stream (.getBytes "1"))})
        (put-object s {:key "public/b.png" :input-stream (bytes->stream (.getBytes "2"))})
        (put-object s {:key "private/c.png" :input-stream (bytes->stream (.getBytes "3"))})
        (put-object s {:key "legacy__x.png" :input-stream (bytes->stream (.getBytes "4"))})
        (is (= ["public/a.png" "public/b.png"]
               (vec (sort (list-objects s "public/")))))
        (is (= ["legacy__x.png" "private/c.png" "public/a.png" "public/b.png"]
               (vec (sort (list-objects s nil))))))
      (finally
        (io/delete-file root true)))))

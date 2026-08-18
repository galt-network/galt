(ns galt.core.adapters.handlers-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [ring.mock.request :as mock]
    [galt.core.adapters.handlers :as handlers]
    [galt.core.infrastructure.asset-stores :as asset-stores]))

(defn- disk-deps [upload-stub]
  {:file-storage (asset-stores/new-file-storage
                   {:type :disk
                    :storage-root (str (System/getProperty "java.io.tmpdir") "/galt-handlers-test")
                    :root-url "http://localhost:8081"})
   :upload-asset-use-case upload-stub})

(deftest store-file-success
  (testing "returns 201 with asset url for a successful upload"
    (let [deps (disk-deps (fn [_command] [:ok {:key "public/abc.png"} nil]))
          req (-> (mock/request :post "/files")
                  (assoc :multipart-params {"uploaded-file" {:filename "a.png"
                                                             :content-type "image/png"
                                                             :size 10
                                                             :tempfile (java.io.File. "/tmp/a.png")}}))
          res (handlers/store-file deps req)]
      (is (= 201 (:status res)))
      (is (= "http://localhost:8081/files/public/abc.png" (:body res))))))

(deftest store-file-validation-error
  (testing "returns 400 with errors for an invalid upload"
    (let [deps (disk-deps (fn [_command] [:error nil ["File too large (max 10 MB)"]]))
          req (-> (mock/request :post "/files")
                  (assoc :multipart-params {"uploaded-file" {:filename "big.mp4"
                                                             :content-type "video/mp4"
                                                             :size 100000000000
                                                             :tempfile (java.io.File. "/tmp/big.mp4")}}))
          res (handlers/store-file deps req)]
      (is (= 400 (:status res)))
      (is (= {:errors ["File too large (max 10 MB)"]} (:body res))))))

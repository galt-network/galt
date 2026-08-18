(ns galt.core.infrastructure.s3-asset-store-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [galt.core.infrastructure.s3-asset-store :as s3]))

(deftest client-endpoint-resolves-for-r2-auto-region
  (testing "aws-api client built from config with R2's :region \"auto\" resolves to the R2 endpoint"
    (let [store (s3/new-s3-asset-store
                 {:endpoint "https://e58cb0720ecc0a067ef67e87eede2b3b.r2.cloudflarestorage.com"
                  :bucket "galt-media-dev"
                  :access-key "test-access-key"
                  :secret-key "test-secret-key"
                  :region "auto"})
          client @(:client store)
          endpoint (:endpoint client)]
      (is (nil? (:cognitect.anomalies/category endpoint))
          "region \"auto\" must be coerced to a resolvable region, not fail with `No known endpoint.`")
      (is (= "e58cb0720ecc0a067ef67e87eede2b3b.r2.cloudflarestorage.com"
             (:hostname endpoint))
          "endpoint-override hostname must win over the resolved AWS endpoint"))))

(deftest client-endpoint-preserves-explicit-region
  (testing "a real region passes through unchanged"
    (let [store (s3/new-s3-asset-store
                 {:endpoint "https://e58cb0720ecc0a067ef67e87eede2b3b.r2.cloudflarestorage.com"
                  :bucket "galt-media-dev"
                  :access-key "test-access-key"
                  :secret-key "test-secret-key"
                  :region "us-west-2"})
          client @(:client store)]
      (is (= "us-west-2" (:region client)))
      (is (nil? (:cognitect.anomalies/category (:endpoint client)))))))
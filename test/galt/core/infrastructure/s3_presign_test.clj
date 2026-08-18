(ns galt.core.infrastructure.s3-presign-test
  (:require
    [clojure.string :as str]
    [clojure.test :refer [deftest is]]
    [galt.core.infrastructure.s3-presign :as presign]))

(deftest sigv4-presigned-url-matches-botocore-reference
  (let [url (presign/presign-url
              {:host "examplebucket.s3.amazonaws.com"
               :uri "/test.txt"
               :region "us-east-1"
               :access-key "AKIDEXAMPLE"
               :secret-key "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY"
               :expires-in-seconds 86400
               :timestamp "20130524T000000Z"})]
    (is (= "https://examplebucket.s3.amazonaws.com/test.txt"
           (subs url 0 (str/index-of url "?"))))
    (is (= "X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=AKIDEXAMPLE%2F20130524%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20130524T000000Z&X-Amz-Expires=86400&X-Amz-SignedHeaders=host&X-Amz-Signature=ca6159ff16837c055653a722d9f10b6a529b7c62c84174a2859958324bc78766"
           (subs url (inc (str/index-of url "?")))))))

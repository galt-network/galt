(ns galt.core.infrastructure.bitcoin.lnurl-test
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]
   [galt.core.infrastructure.bitcoin.lnurl :as subject]))

(deftest generate-lnurl-test
  (let [{:keys [lnurl url k1-hex]}
        (subject/generate-lnurl "https://galt.is" "/lnurl/auth" {:token "abc"})]
    (is (str/starts-with? lnurl "lnurl"))
    (is (str/starts-with? url "https://galt.is/lnurl/auth"))
    (is (str/includes? url "tag=login"))
    (is (str/includes? url "token=abc"))
    (is (re-matches #"[0-9a-f]{64}" k1-hex))))

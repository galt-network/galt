(ns galt.core.infrastructure.bitcoin.lnurl-test
  (:require
   [clojure.test :refer [deftest is]]
   [galt.core.infrastructure.bitcoin.lnurl :as subject]))

(deftest generate-something-test
  (is (= true (subject/generate-something))))

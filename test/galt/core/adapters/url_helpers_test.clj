(ns galt.core.adapters.url-helpers-test
  (:require
    [clojure.test :refer [deftest is]]
    [galt.core.adapters.url-helpers :refer [add-query-params]]))


(deftest add-query-params-test
  (is (= "/hello?q=wut&return-to=/members/me"
         (-> "/hello?q=wut"
             (add-query-params ,,, {:return-to "/members/me"})))))

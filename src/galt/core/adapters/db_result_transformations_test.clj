(ns galt.core.adapters.db-result-transformations-test
  (:require
   [clojure.test :refer [deftest is]]
   [galt.core.adapters.db-result-transformations :as subject]))

(deftest galt.core.adapters.db-result-transformations-test
  (is (= {:my-id 42}
         (subject/transform-row {:table/my-id [(comp keyword name)]}
                                {:table/my-id 42 :table/not-there 21}))))

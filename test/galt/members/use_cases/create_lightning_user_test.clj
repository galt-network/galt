(ns galt.members.use-cases.create-lightning-user-test
  (:require
    [clojure.test :refer [deftest testing is]]
    [matcher-combinators.test]
    [spy.core :as spy]
    [spy.assert :as assert]
    [spy.test]
    [galt.members.use-cases.create-lightning-user :refer [create-lightning-user-use-case]]))

(deftest create-lightning-user-test
  (is (fn? create-lightning-user-use-case)))

(ns galt.groups.domain.entities.group
  (:require [clojure.spec.alpha :as s]))

(s/def ::min-length-string
  (s/and string? #(>= (count %) 5)))

(s/def ::group
  (s/keys :req-un [::name ::description]))

(s/def ::name ::min-length-string)
(s/def ::description ::min-length-string)

(comment
  (def valid-group {:name "Clojure" :description "Functional programming"})
  (def invalid-group1 {:name "Cloj" :description "Fun"}) ; Too short
  (def invalid-group2 {:name "Clojure"}) ; Missing key

  (println "Valid group:" (s/valid? ::group valid-group)) ; true
  (println "Invalid group1:" (s/valid? ::group invalid-group1)) ; false
  (println "Invalid group2:" (s/valid? ::group invalid-group2)) ; false

  (s/explain ::group invalid-group1)
  (s/explain ::group invalid-group2))

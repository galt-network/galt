(ns galt.groups.domain.entities.group
  (:require
    [clojure.spec.alpha :as s])
  (:import
    [java.time LocalDateTime]))

(s/def ::id uuid?)
(s/def ::name (s/and string? #(>= (count %) 5)))
(s/def ::avatar any?)
(s/def ::description (s/and string? #(>= (count %) 30)))
(s/def ::created-at #(instance? LocalDateTime %))

(s/def ::group
  (s/keys :req-un [::id ::name ::avatar ::description ::created-at]))

(defrecord Group [id name description created-at])

(defn new-group
  [{:keys [id name description avatar created-at]}]
  {:post [#(s/assert ::group %)]}
  (map->Group {:id id :name name :description description :avatar avatar :created-at created-at}))

(comment
  (def valid-group {:name "Clojure" :description "Functional programming"})
  (def invalid-group1 {:name "Cloj" :description "Fun"}) ; Too short

  (println "Valid group:" (s/valid? ::group valid-group)) ; true
  (println "Invalid group1:" (s/valid? ::group invalid-group1)) ; false

  (s/explain ::group invalid-group1)
  )

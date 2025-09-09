(ns galt.groups.domain.use-cases.add-group
  (:require
    [clojure.spec.alpha :as s]))

(def max-group-per-member 100)

(defn within-group-creation-limits?
  [{:keys [find-groups-by-founder-id]} {:keys [founder-id]}]
  (>= max-group-per-member (count (find-groups-by-founder-id founder-id))))

(defn active-member?
  [_deps _group-creation]
  true)

(defn unique-name?
  [{:keys [find-groups-by-name]} group-creation]
  (empty? (find-groups-by-name (:name group-creation))))

(def requirements
  [[within-group-creation-limits? "Maximum number of groups per member reached"]
   [active-member? "User must have an active membership on GALT"]
   [unique-name? "There already is a group with that name"]])

(defn validate-all
  [deps requirements group-creation]
  (vec (keep (fn [[pred message]]
               (when-not (pred deps group-creation) message))
             requirements)))

(s/def ::founder-id uuid?)
(s/def ::name string?)
(s/def ::description string?)
(s/def ::location map?)
(s/def ::command (s/keys :req-un [::founder-id ::name ::description ::location]))

(defn add-group-use-case
  [{:keys [gen-uuid
           find-group-by-id
           find-groups-by-founder-id
           find-groups-by-name
           add-group
           add-location] :as deps} command]
  (s/assert ::command command)
  (let [group-params (dissoc command :location)
        location-params (:location command)
        validation-errors (validate-all deps requirements group-params)
        founder-id (:founder-id command)
        group-fields (-> command
                         (select-keys ,,, [:name :description :avatar])
                         (assoc ,,, :id (gen-uuid)))
        ; FIXME Shouldn't create location when there are validation errors
        created-location (add-location location-params)
        group (assoc group-fields :location-id (:id created-location))
        ; FIXME Shouldn't create group when there are validation errors
        created-group (add-group founder-id group)]
    (if (empty? validation-errors)
      [:ok created-group nil]
      [:error nil validation-errors])))

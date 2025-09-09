(ns galt.groups.domain.use-cases.add-group
  (:require
    [clojure.spec.alpha :as s]
    [galt.groups.domain.entities.group :as entities.group]
    [galt.groups.domain.group-repository :as gr :refer [add-group]]
    [galt.locations.domain.location-repository :as lr]))

(def max-group-per-member 100)

(defn within-group-creation-limits?
  [{:keys [group-repo]} {:keys [founder-id]}]
  (>= max-group-per-member (count (gr/find-groups-by-founder-id group-repo founder-id))))

(defn active-member?
  [{:keys [member-repo]} group-creation]
  true)

(defn unique-name?
  [{:keys [group-repo]} group-creation]
  (empty? (gr/find-groups-by-name group-repo (:name group-creation))))

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
  [{:keys [group-repo location-repo gen-uuid] :as deps} command]
  (s/assert ::command command)
  (let [group-params (dissoc command :location)
        location-params (:location command)
        validation-errors (validate-all deps requirements group-params)
        founder-id (:founder-id command)
        group-fields (-> command
                         (select-keys ,,, [:name :description :avatar])
                         (assoc ,,, :id (gen-uuid)))
        ; FIXME Shouldn't create location when there are validation errors
        created-location (lr/add-location location-repo location-params)
        group (assoc group-fields :location-id (:id created-location))
        ; FIXME Shouldn't create group when there are validation errors
        created-group (add-group group-repo founder-id group)]
    (if (empty? validation-errors)
      [:ok created-group nil]
      [:error nil validation-errors])))

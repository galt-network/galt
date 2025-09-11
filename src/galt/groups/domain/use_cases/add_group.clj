(ns galt.groups.domain.use-cases.add-group
  (:require
    [clojure.spec.alpha :as s]))

(def max-group-per-member 100)

(defn within-group-creation-limits?
  [{:keys [find-groups-by-founder-id]} {:keys [founder-id]}]
  (>= max-group-per-member (count (find-groups-by-founder-id founder-id))))

(defn active-member?
  [{:keys [find-member-by-user-id]} {:keys [founder-id]}]
  (not (nil? (find-member-by-user-id founder-id))))

(defn unique-name?
  [{:keys [find-groups-by-name]} group-creation]
  (empty? (find-groups-by-name (:name group-creation))))

(def requirements
  [[active-member? "User must have an active membership on GALT"]
   [within-group-creation-limits? "Maximum number of groups per member reached"]
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

(defn create-group
  [{:keys [gen-uuid add-location add-group]} command]
  (let [founder-id (:founder-id command)
        location-params (:location command)
        group-fields (-> command
                         (select-keys ,,, [:name :description :avatar])
                         (assoc ,,, :id (gen-uuid)))
        created-location (add-location location-params)
        group (assoc group-fields :location-id (:id created-location))]
    (add-group founder-id group)))

(defn add-group-use-case
  [deps command]
  (s/assert ::command command)
  (let [group-params (dissoc command :location)
        validation-errors (validate-all deps requirements group-params)]
    (if (empty? validation-errors)
      [:ok (create-group deps command) nil]
      [:error nil validation-errors])))

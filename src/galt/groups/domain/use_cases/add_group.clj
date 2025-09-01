(ns galt.groups.domain.use-cases.add-group
  (:require
    [clojure.spec.alpha :as s]
    [galt.groups.domain.entities.group :as entities.group]
    [galt.groups.domain.group-repository :as gr :refer [add-group]]))

(defn within-group-creation-limits?
  [{:keys [group-repo]} {:keys [founder-id]}]
  (>= 3 (count (gr/find-groups-by-founder-id group-repo founder-id))))

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
(s/def ::command (s/keys :req-un [::founder-id ::name ::description]))

(defn new-add-group-use-case
  [deps]
  (fn [command]
    (s/assert ::command command)
    (let [validation-errors (validate-all deps requirements command)
          group-repo (:group-repo deps)
          uuid (:uuid deps)
          founder-id (:founder-id command)
          group-fields (-> command
                           (select-keys ,,, [:name :description :avatar])
                           (assoc ,,, :id (uuid)))
          group (entities.group/new-group group-fields)]
      (if (empty? validation-errors)
        [:ok (add-group group-repo founder-id group) nil]
        [:error nil validation-errors]))))

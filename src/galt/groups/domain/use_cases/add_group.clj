(ns galt.groups.domain.use-cases.add-group
  (:require
    [galt.groups.domain.group-repository :refer [create-group]]))

(defn- can-create-group?
  [member-id]
  (= member-id (parse-uuid "38da67bd-ee75-4a71-b70c-618ac1053ec7")))

; Conditions for creating group
;  - one group for free: can create it when is a member, if deletes it, the slot can be used again
;  - next groups cost 1000 SAT-s to create
;  - hasn't surpassed max group creation limit per member: 10
(defn new-add-group-use-case
  [group-repo]
  (fn [creator-id group-name group-description]
    (if (can-create-group? creator-id)
      [:ok (create-group group-repo creator-id group-name group-description)]
      [:error "Couldn't create a group"])))

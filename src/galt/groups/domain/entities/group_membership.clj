(ns galt.groups.domain.entities.group-membership)

(defrecord GroupMembership [member-id group-id role])

(defn new-group-membership [member-id group-id role]
  (->GroupMembership member-id group-id (keyword role)))

(defn allowed-action?
  [membership action]
  (case action
        :edit (#{:admin :founder} (:role membership))
        :view true))

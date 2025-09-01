(ns galt.groups.domain.group-repository)

(defprotocol GroupRepository
  (add-group [this creator-id group])
  (find-group-by-id [this group-id])
  (find-groups-by-name [this name])
  (find-groups-by-founder-id [this founder-id])
  (update-group [this id name description])
  (add-to-group [this group-id member-id role])
  (find-groups-by-member [this member-id])
  (list-members [this group-id params])
  (list-groups [this])
  (find-membership-by-member [this group-id member-id]))

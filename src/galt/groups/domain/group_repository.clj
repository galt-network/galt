(ns galt.groups.domain.group-repository)

(defprotocol GroupRepository
  (create-group [this creator-id group-name group-description])
  (find-group-by-id [this group-id])
  (update-group [this id name description])
  (add-to-group [this group-id member-id role])
  (find-groups-by-member [this member-id])
  (list-members [this group-id params])
  (list-groups [this]))

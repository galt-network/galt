(ns galt.groups.domain.group-repository)

(defprotocol GroupRepository
  (add-group [this creator-id group])
  (find-group-by-id [this group-id])
  (update-group [this group])
  (add-to-group [this group-id member-id role])
  (find-groups-by-member [this member-id])
  (list-members [this group-id params])
  (list-groups [this params])
  (find-membership-by-member [this group-id member-id])
  (delete-group [this group-id]))

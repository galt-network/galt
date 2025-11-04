(ns galt.members.domain.member-repository)

(defprotocol MemberRepository
  (add-member [this member])
  (update-member [this member-id attributes])
  (find-members-by-name [this s] [this s group-id])
  (find-member-by-id [this id])
  (find-member-by-user-id [this id])
  (list-members [this] [this params])
  (fuzzy-find-member [this s] [this s group-id]))

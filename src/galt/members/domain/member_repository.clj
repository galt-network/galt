(ns galt.members.domain.member-repository)

(defprotocol MemberRepository
  (add-member [this member])
  (find-member-by-id [this id])
  (find-member-by-user-id [this id])
  (list-members [this] [this params])
  (fuzzy-find-member [this s]))

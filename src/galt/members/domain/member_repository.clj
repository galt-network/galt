(ns galt.members.domain.member-repository)

(defprotocol MemberRepository
  (add-member [[this id user-id]]))

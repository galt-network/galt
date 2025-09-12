(ns galt.invitations.domain.invitation-repository)

(defprotocol InvitationRepository
  (invitation-by-id [this id])
  (add-invitation-request [this invitation-request])
  (user-invitation-requests [this user-id])
  (invitations-by-member [this member-id])
  (add-invitation [this invitation]))

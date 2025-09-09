(ns galt.invitations.domain.invitation-repository)

(defprotocol InvitationRepository
  (add-invitation-request [this invitation-request])
  (user-invitation-requests [this user-id]))

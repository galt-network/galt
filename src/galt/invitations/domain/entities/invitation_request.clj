(ns galt.invitations.domain.entities.invitation-request)

(defrecord InvitationRequest [id requesting-user-id target-member-id target-group-id email content])

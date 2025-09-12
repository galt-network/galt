(ns galt.invitations.domain.entities.invitation)

(defrecord Invitation [id
                       inviting-member-id
                       target-group-id
                       content
                       created-at
                       expires-at
                       max-usages
                       current-usages])

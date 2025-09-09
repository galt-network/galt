(ns galt.invitations.adapters.db-invitation-repository
  (:require
    [galt.invitations.domain.invitation-repository :refer [InvitationRepository]]
    [galt.core.adapters.db-access :refer [query]]))

(defrecord DbInvitationRepository [db-access]
  InvitationRepository
  (add-invitation-request [_ invitation-request]
    (query db-access {:insert-into [:invitation_requests] :values [(dissoc invitation-request :id)]}))

  (user-invitation-requests [_ user-id]
    (query db-access {:select [:*] :from [:invitation_requests] :where [:= :requesting_user_id user-id]})))

(defn new-db-invitation-repository [db-access]
  (DbInvitationRepository. db-access))

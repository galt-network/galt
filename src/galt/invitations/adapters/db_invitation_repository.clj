(ns galt.invitations.adapters.db-invitation-repository
  (:require
    [galt.invitations.domain.invitation-repository :refer [InvitationRepository]]
    [galt.core.adapters.db-access :refer [query query-one]]
    [galt.invitations.domain.entities.invitation :refer [map->Invitation]]
    [galt.core.adapters.db-result-transformations :refer [transform-row defaults ->local-date-time]]))

(def invitation-spec
  {:invitations/id defaults
   :invitations/inviting_member_id defaults
   :invitations/target_group_id defaults
   :invitations/content defaults
   :invitations/created_at [(first defaults) ->local-date-time]
   :invitations/expires_at [(first defaults) ->local-date-time]
   :invitations/max_usages defaults
   :invitations/current_usages defaults})

(defrecord DbInvitationRepository [db-access]
  InvitationRepository

  (invitation-by-id [_ id]
    (some->> {:select [:*] :from [:invitations] :where [:= :id id]}
             (query-one db-access ,,,)
             (transform-row invitation-spec ,,,)
             (map->Invitation ,,,)))

  (add-invitation-request [_ invitation-request]
    (query db-access {:insert-into [:invitation_requests] :values [(dissoc invitation-request :id)]}))

  (user-invitation-requests [_ user-id]
    (query db-access {:select [:*] :from [:invitation_requests] :where [:= :requesting_user_id user-id]}))

  (invitations-by-member [_ member-id]
    (some->> {:select [:*] :from [:invitations] :where [:= :inviting_member_id member-id]}
             (query db-access ,,,)
             (map #(transform-row invitation-spec %) ,,,)
             (map map->Invitation ,,,)))

  (add-invitation [_ invitation]
    (some->> {:insert-into [:invitations] :values [invitation] :returning [:*]}
             (query-one db-access ,,,)
             (transform-row invitation-spec ,,,)
             (map->Invitation ,,,))))

(defn new-db-invitation-repository [db-access]
  (DbInvitationRepository. db-access))

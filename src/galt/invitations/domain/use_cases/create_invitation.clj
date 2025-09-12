(ns galt.invitations.domain.use-cases.create-invitation
  (:require
    [galt.invitations.domain.entities.invitation-request :refer [->InvitationRequest]]))

(defn create-invitation-use-case
  [{:keys [add-invitation]}
   {:keys [invitation] :as command}]
  (let []
    [:ok (add-invitation invitation)]))

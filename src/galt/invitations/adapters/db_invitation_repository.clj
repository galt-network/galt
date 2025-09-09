(ns galt.invitations.adapters.db-invitation-repository
  (:require
    [next.jdbc :refer [execute!]]
    [galt.core.adapters.db-access :refer [query]]))

(defn add-invitation-request [conn invitation-request]
  (query conn {:insert-into [:invitation_requests] :values [(dissoc invitation-request :id)]}))

(defn user-invitation-requests [conn user-id]
  (query conn {:select [:*] :from [:invitation_requests] :where [:= :requesting_user_id user-id]}))

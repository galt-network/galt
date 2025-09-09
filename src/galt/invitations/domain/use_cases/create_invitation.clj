(ns galt.invitations.domain.use-cases.create-invitation
  (:require
    [clojure.spec.alpha :as s]
    [clojure.spec.test.alpha :as st]
    [galt.invitations.domain.entities.invitation-request :refer [->InvitationRequest]]))

(s/def ::deps (s/keys :req-un [::find-user-by-id
                               ::user-invitation-requests
                               ::add-invitation-request]))

(s/def ::command (s/keys :req-un [::from-user-id
                                  ::to-group-id
                                  ::to-member-id
                                  ::email
                                  ::content]))
(defn create-invitation-use-case
  [{:keys [find-user-by-id
           user-invitation-requests
           add-invitation-request]}
   {:keys [from-user-id to-member-id to-group-id email content] :as command}]
  (s/assert ::command command)
  (let [user (find-user-by-id from-user-id)]
    (if (nil? user)
      [:error "Unknown user"]
      (if (> (count (user-invitation-requests from-user-id)) 3)
        [:error "Over the limit of invitation requests"]
        (if (< (count content) 30)
          [:error "Request content too short (at least 30 characters)"]
          [:ok (add-invitation-request (->InvitationRequest nil
                                                            from-user-id
                                                            to-member-id
                                                            to-group-id
                                                            email
                                                            content))])))))


(s/fdef create-invitation-use-case
        :args (s/keys :req [::deps ::command]))

(st/instrument 'create-invitation-use-case)


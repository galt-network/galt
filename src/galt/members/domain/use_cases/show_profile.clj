(ns galt.members.domain.use-cases.show-profile
  (:require
    [clojure.core.match :refer [match]]
    ))
; Data needed:
;   - posts (show only titles of first 5, & produce link to posts page with search filter for that user)
;   - created events
;   - events participated by (if user configures to make his participation public)
;   - comments posted (show 5 most recent or popular)
;   - separate section for messages (only when user is logged in)
(defn show-profile-use-case
  [{:keys [find-member-by-id
           find-groups-by-member
           find-location-by-id
           current-membership-payment]}
   {:keys [member-id user-id]}]
  (let [existing-member (find-member-by-id member-id)
        membership-payment (when user-id (current-membership-payment user-id))]
    (match [membership-payment existing-member]
           [nil nil] [:error "Profile not found"]
           [_   nil] [:ok {:member nil}]
           [_   _  ] [:ok {:member existing-member
                           :location (find-location-by-id (:location-id existing-member))
                           :groups (find-groups-by-member (:id existing-member))}])))

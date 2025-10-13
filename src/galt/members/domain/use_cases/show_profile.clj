(ns galt.members.domain.use-cases.show-profile
  (:require
    [clojure.core.match :refer [match]]
    ))

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

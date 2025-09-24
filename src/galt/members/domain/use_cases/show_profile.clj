(ns galt.members.domain.use-cases.show-profile
  (:require
    [clojure.core.match :refer [match]]
    ))

(defn show-profile-use-case
  [{:keys [find-member-by-user-id
           find-groups-by-member
           find-location-by-id
           current-membership-payment]}
   {:keys [user-id]}]
  (let [membership-payment (current-membership-payment user-id)
        member (find-member-by-user-id user-id)]
    (match [membership-payment member]
           [nil nil] [:error "Profile not found"]
           [_   nil] [:ok {:member nil}]
           [_   _  ] [:ok {:member member
                           :location (find-location-by-id (:location-id member))
                           :groups (find-groups-by-member (:id member))}])))

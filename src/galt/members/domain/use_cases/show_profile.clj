(ns galt.members.domain.use-cases.show-profile)

(defn show-profile-use-case
  [{:keys [find-member-by-id
           find-groups-by-member
           find-location-by-id]}
   {:keys [member-id]}]
  (let [member (find-member-by-id member-id)]
    (if (nil? member)
      [:error "Profile not found"]
      [:ok {:member member
            :location (find-location-by-id (:location-id member))
            :groups (find-groups-by-member member-id)}])))

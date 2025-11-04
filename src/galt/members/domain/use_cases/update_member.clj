(ns galt.members.domain.use-cases.update-member)

;; If parameter value didn't change, do nothing with it
(defn update-member-use-case
  [{:keys [update-member find-member-by-id add-location update-location]}
   {:keys [member location] :as command}]
  (let [member-id (:id member)
        db-member (find-member-by-id member-id)
        location-id (:location-id db-member)
        member-params (select-keys member [:id :name :slug :avatar :description])
        location-params (select-keys location [:name :latitude :longitude :country-code :city-id])
        updated-location (if (nil? location-id)
                           (add-location location-params)
                           (update-location location-id location-params))
        updated-member (update-member member-id (assoc member-params :location-id (:id updated-location)))]
    (if updated-member
      [:ok updated-member]
      [:error "There was an error updating this member"])))

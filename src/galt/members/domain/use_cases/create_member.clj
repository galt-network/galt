(ns galt.members.domain.use-cases.create-member)

(defn create-member-use-case
  [{:keys [add-member add-location]} {:keys [member location]}]
  (let [member-params (select-keys member [:user-id :name :slug :avatar :description])
        location-params (select-keys location [:name :latitude :longitude :country-code :city-id])
        added-location (add-location location-params)
        added-member (add-member (assoc member-params :location-id (:id added-location)))]
    (if added-member
      [:ok added-member]
      [:error "There was an error creating member"])))

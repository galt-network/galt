(ns galt.groups.domain.use-cases.show-group)

(def actions
  [{:title "New Event"
    :href "/events/new"
    :can? (fn [user-id group-membership] ())}])

(defn show-group-use-case
  [{:keys [find-group-by-id
           find-location-by-id
           list-posts
           list-members]}
   {:keys [viewing-user-id group-id]}]
  (let [group (find-group-by-id group-id)
        location (find-location-by-id (:location-id group))
        members (list-members group-id {:limit 5})
        posts (list-posts {:group-id group-id})
        actions ()]
    [:ok {:group group :location location :members members :posts posts}]))

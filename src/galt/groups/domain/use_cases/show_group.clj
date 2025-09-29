(ns galt.groups.domain.use-cases.show-group)

(defn show-group-use-case
  [{:keys [find-group-by-id
           find-location-by-id
           group-posts
           list-members]}
   {:keys [group-id]}]
  (let [group (find-group-by-id group-id)
        location (find-location-by-id (:location-id group))
        members (list-members group-id {:limit 5})
        posts (group-posts group-id)]
    [:ok {:group group :location location :members members :posts posts}]))

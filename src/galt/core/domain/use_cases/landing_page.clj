(ns galt.core.domain.use-cases.landing-page)

(defn landing-page-use-case
  [{:keys [count-members count-groups count-events
           list-recent-groups list-recent-posts list-recent-events
           find-groups-by-member]}
   {:keys [member-id]}]
  [:ok {:stats {:members (count-members)
                :groups (count-groups)
                :events (count-events)}
        :recent-groups (list-recent-groups {:limit 5})
        :recent-posts (list-recent-posts {:limit 5})
        :recent-events (list-recent-events {:limit 5})
        :my-groups (when member-id (find-groups-by-member member-id))}])

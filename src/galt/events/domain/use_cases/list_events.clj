(ns galt.events.domain.use-cases.list-events)

(defn list-events-use-case
  [{:keys [list-events]}
   {:keys [limit offset from-date to-date type]}]
  [:ok (list-events {:limit limit
                     :offset offset
                     :from-date from-date
                     :to-date to-date
                     :type type})])

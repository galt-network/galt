(ns galt.groups.domain.use-cases.new-group)

(defn new-group-use-case
  [{:keys [find-member-by-user-id]} {:keys [user-id]}]
  (let [member (find-member-by-user-id user-id)]
    (if (nil? member)
      [:error "Must be a member to create groups"]
      [:ok member])))

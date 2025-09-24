(ns galt.invitations.domain.use-cases.invitation-dashboard
  (:require
    [galt.core.adapters.db-result-transformations :refer [transform-row defaults ->local-date-time]]
    [java-time.api :refer [local-time with-zone]]
    ))

(defn active-invitations
  [member-id]
  {:select [:invitations.id
            :members.name
            :groups.name
            :invitations.created_at
            :invitations.expires_at
            :invitations.max_usages
            :invitations.current_usages]
   :from [:invitations]
   :join [:members [:= :members.id :invitations.inviting_member_id]]
   :left-join [:groups [:= :groups.id :invitations.target_group_id]]
   :where [:and
           [:= :invitations.inviting_member_id member-id]]})

(defn invitation-spec
  []
  {:invitations/id defaults
   :members/name [:inviting-member identity]
   :groups/name [(constantly :to-group) identity]
   :invitations/created_at [(first defaults) ->local-date-time]
   :invitations/expires_at [(first defaults) ->local-date-time]
   :invitations/max_usages defaults
   :invitations/current_usages defaults})

(defn invitation-dashboard-use-case
  [{:keys [query find-groups-by-member pub-key->name]} {:keys [member-id]}]
  (let [member-groups (find-groups-by-member member-id)
        active (query (update-in (active-invitations member-id)
                                             [:where] conj [:> :invitations.expires-at :%now]))
        inactive (query (update-in (active-invitations member-id)
                                             [:where] conj [:<= :invitations.expires-at :%now]))]
    [:ok {:active (map #(transform-row (invitation-spec) %) active)
          :inactive (map #(transform-row (invitation-spec) %) inactive)}]))

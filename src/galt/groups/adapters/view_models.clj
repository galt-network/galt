(ns galt.groups.adapters.view-models
  (:require
    [galt.groups.domain.group-repository :as gr]
    [galt.core.adapters.db-access :refer [query]]
    [galt.core.adapters.time-helpers :as time-helpers]))

(defn d*-action
  "Returns a map like
  (d*-action {:groups/id 123} :get :edit) #=> {:data-on-click \"@get('/users/123/edit')\"}"
  [model method & [action]]
  {:data-on-click
   (str "@" (name method) "('/groups/"
        (:groups/id model)
        (when action (str "/" (name action)))
        "')")})

(defn groups-view-model
  [deps _req]
  (let [groups (gr/list-groups (:group-repo deps))
        group-counts {:select [:group_id [[:count :*] :members_count]]
                      :from [:group_memberships]
                      :group-by [:group_id]}
        counts (reduce
                 (fn [acc c] (assoc acc (:group_memberships/group_id c) (:members_count c)))
                 {}
                 (query (:db-access deps) group-counts))
        add-counts (fn [g] (assoc g :members (get counts (:groups/id g))))
        add-action (fn [g] (assoc g :actions [{:name "View" :action (d*-action g :get)}
                                              {:name "Edit" :action (d*-action g :get :edit)}
                                              {:name "Join" :action (d*-action g :post :join)}]))
        groups-with-extras (->> groups
                                (map add-counts ,,,)
                                (map add-action))]
    {:columns [["Group Name" :groups/name] :groups/description :members :actions]
     :groups groups-with-extras}))

(defn group-model
  [{:keys [group-repo galt-url]} _req & [group-id]]
  (let [group-id (parse-uuid group-id)
        group (gr/find-group-by-id group-repo group-id)
        founded-at (.toLocalDateTime (:groups/created_at group))
        members (gr/list-members group-repo group-id {:limit 5})]
    {:name (:groups/name group)
     :description (:groups/description group)
     :languages ["Spanish" "English"]
     :location-name "Córdoba, Argentina"
     :founded-at (str (time-helpers/relative-time founded-at) " (" (time-helpers/short-format founded-at) ")")
     :members (map (fn [m] {:name (:users/name m)
                            :href (str galt-url "/members/" (:users/id m))})
                   members)}))

(ns galt.groups.external.routes
  (:require
    [reitit.ring :as rr]
    [galt.groups.adapters.handlers :as groups]))

(defn router
  [deps]
  (let [with-layout (:with-layout deps)
        with-deps-layout (partial with-layout deps)]
    (rr/router
      [["/groups" {:id :groups
                   :name :groups
                   :get (with-deps-layout groups/list-groups)
                   :post {:handler (with-deps-layout groups/create-group)
                          :min-role :member}}]
       ["/groups/search" {:name :groups/search
                          :conflicting true
                          :get (partial groups/search-groups deps)}]
       ["/groups/new" {:id :groups
                       :name :groups/new
                       :conflicting true
                       :get (with-deps-layout groups/new-group)
                       :min-role :member}]
       ["/groups/:id" {:id :groups
                       :name :groups/by-id
                       :conflicting true
                       :get {:handler (with-deps-layout groups/show-group)}
                       :put (with-deps-layout groups/update-group)
                       :delete (with-deps-layout groups/delete-group)}]
       ["/groups/:id/edit" {:id :groups
                            :name :groups/edit-group
                            :min-role :member
                            :get (with-deps-layout groups/edit-group)}]])))

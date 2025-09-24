(ns galt.design.routes
  (:require
    [reitit.ring :as rr]
    [galt.design.handlers :as handlers]))

(defn router
  [deps]
  (let [with-layout (:with-layout deps)
        with-deps-layout (partial with-layout deps)]
    (rr/router
      [["/design/landing" {:id :groups
                           :name :design/landing
                           :get (with-deps-layout handlers/landing)}]
       ["/design/edit-group" {:id :groups
                              :name :design/edit-group
                              :get (with-deps-layout handlers/edit-group)}]
       ["/design/show-group" {:id :groups
                              :name :design/show-group
                              :get (with-deps-layout handlers/show-group)}]])))

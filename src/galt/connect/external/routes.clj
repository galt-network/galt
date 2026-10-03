(ns galt.connect.external.routes
  (:require
    [galt.connect.adapters.handlers :as handlers]
    [reitit.ring :as rr]))

(defn router
  [deps]
  (let [with-layout (:with-layout deps)
        with-deps-layout (partial with-layout deps)]
    (rr/router
      [["/connect" {:id :connect
                    :name :connect
                    :get (with-deps-layout handlers/show-connect)}]])))

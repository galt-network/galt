(ns galt.world-map.external.routes
  (:require
   [is.galt.globo.server.handlers :as globo-handlers]
   [is.galt.globo.server.middleware :as globo-middleware]
   [reitit.ring :as rr]
   [galt.world-map.adapters.handlers :as handlers]))

(defn- reitit-assets-handler
  "Adapts globo's assets-handler (expects sequential :path-params) to
  reitit's map form."
  [req]
  (globo-handlers/assets-handler (update req :path-params vals)))

(defn router
  [deps]
  (let [mount-path (or (:globo-mount-path deps) "/world-map")
        with-world-map-layout (partial handlers/with-world-map-layout deps)
        globo-deps {:storage (:globo-storage deps)
                    :sse-clients (:globo-sse-clients deps)}
        globo-connection-handler (-> (partial globo-handlers/new-connection-handler globo-deps)
                                     handlers/wrap-globo-user-id
                                     globo-middleware/wrap-error-response)
        globo-send-message-handler (-> (partial globo-handlers/send-message-handler globo-deps)
                                       handlers/wrap-globo-user-id
                                       globo-middleware/wrap-error-response)]
    (rr/router
     [[(str mount-path)
       {:id :world-map
        :name :world-map
        :get (with-world-map-layout handlers/world-map-page)}]
      [(str mount-path "/connection")
       {:get globo-connection-handler}]
      [(str mount-path "/send-message")
       {:post globo-send-message-handler}]
      [(str mount-path "/assets/*path")
       {:name :world-map/assets
        :conflicting true
        :get reitit-assets-handler}]])))

(ns galt.world-map.external.routes
  (:require
   [is.galt.globo.server.handlers :as globo-handlers]
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
        globo (:globo deps)
        globo-connection-handler (-> deps
                                     (handlers/wrap-globo-user-name
                                      (partial globo-handlers/new-connection-handler globo))
                                     handlers/wrap-globo-user-id
                                     handlers/wrap-error-logging)
        globo-send-message-handler (-> (partial globo-handlers/send-message-handler globo)
                                       handlers/wrap-globo-user-id
                                       handlers/wrap-error-logging)]
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

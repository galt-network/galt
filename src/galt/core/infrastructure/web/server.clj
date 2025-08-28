(ns galt.core.infrastructure.web.server
  (:require
    [org.httpkit.server :as hk-server]))

(defn start! [handler & {:as opts}]
  (let [opts (merge {:port 8081 :join? false}
                    opts)]
    (println "Starting server with opts:" opts "with handler:" handler)
    (hk-server/run-server handler opts)))

(defn stop! [server]
  (println "Stopping server" server)
  (deref (hk-server/server-stop! server))
  (hk-server/server-join server))

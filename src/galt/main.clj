(ns galt.main
  (:require
    [nrepl.server :as nrepl]
    [nrepl.cmdline]
    [galt.core.system :as system]
    [cider.nrepl :refer [cider-nrepl-handler]]
    [clojure.core.async :as async]
    [galt.core.infrastructure.db-migrations :as db-migrations]
    [galt.core.infrastructure.version])
  (:gen-class))

(defonce nrepl-server (atom nil))

(defn start-nrepl!
  [{:keys [port bind] :as opts :or {port 7888 bind "localhost"}}]
  (let [server (nrepl/start-server :port port :bind bind :handler cider-nrepl-handler)]
    (println "Started nrepl server on port" port)
    (nrepl.cmdline/save-port-file server opts)
    server))

(def stop-ch (async/chan))

(def running-system (atom nil))

(defn start-system!
  ([] (start-system! (keyword (System/getenv "GALT_ENV"))))
  ([name]
   (println "Starting GALT system" name)
   (reset! running-system (system/start! name))))

(defn stop-system! []
  (when @running-system (system/stop! @running-system))
  (reset! running-system nil))

(defn before-ns-unload []
  (stop-system!))

(defn after-ns-reload []
  (start-system!))

(defn -main [& args]
  ; (reset! nrepl-server (start-nrepl! {:port 7888}))
  (case (first args)
    "init"
    (do
      (println "Setting up database")
      (db-migrations/init!))
    "migrate"
    (do
      (println "Running database migrations")
      (db-migrations/migrate!))
    ; If no commands given, start the system
    (do
      (println "Starting the GALT system")
      (start-system!)
      (future (async/<!! stop-ch)))))

(comment
  (require '[galt.groups.domain.group-repository :refer [list-groups find-groups-by-member]])
  (def repo (get-in @running-system [:donut.system/instances :storage :group]))
  (find-groups-by-member repo 42)
  (get-in @running-system [:donut.system/instances :use-cases])
  )

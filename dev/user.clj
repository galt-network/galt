(ns user
  (:require
    [clj-reload.core :as reload]
    [galt.main]
    [galt.core.system]))

(alter-var-root #'*warn-on-reflection* (constantly true))

(reload/init
  {:no-reload '#{user}})

(defn go! []
  (reload/reload))

(comment
  (go!)
  (galt.main/start-system! :dev)

  (require '[sci.nrepl.browser-server :as nrepl])
  (nrepl/start! {:nrepl-port 1339 :websocket-port 1340})

  (keys @galt.main/running-system)
  (require '[galt.members.domain.user-repository :refer [list-users]])
  (require '[galt.members.adapters.db-user-repository :refer [new-db-user-repository]])
  (require '[clj-uuid])
  (clj-uuid/v7)

  (list-users
    (new-db-user-repository (get-in @galt.main/running-system [:donut.system/instances :storage :db])))
  *e)

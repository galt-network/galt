(ns galt.core.system
  (:require
    [donut.system :as ds]
    [galt.core.infrastructure.database :as database]
    [galt.core.infrastructure.web.middleware :as middleware]
    [galt.core.infrastructure.name-generator :as name-generator]
    [galt.core.infrastructure.web.routes :as routes]
    [galt.core.infrastructure.web.server :as web.server]
    [galt.core.infrastructure.disk-file-storage :as file-storage]
    [galt.groups.adapters.handlers]
    ; [galt.core.adapters.db-access :as db-access]
    [galt.core.adapters.postgres-db-access :refer [new-db-access]]
    [galt.groups.adapters.db-group-repository :as group-repository]
    [galt.members.adapters.db-user-repository :as db-user-repository]
    [clojure.java.io]
    [clojure.edn])
  (:import
   [com.zaxxer.hikari HikariDataSource]))


(defn get-config
  ([]
   (get-config (or (System/getenv "GALT_CONFIG") "config/default-config.edn")))
  ([config-path]
   (-> config-path
       clojure.java.io/file
       slurp
       clojure.edn/read-string)))

(def system
  {::ds/defs
   {:env {}
    :storage
    {:db
     #::ds{:start
           (fn [{:keys [::ds/config]}] (database/create-pooled-datasource (database/make-config config)))
           :stop
           (fn [{::ds/keys [^HikariDataSource instance]}] (.close instance))
           :config
           {:user (System/getenv "MIGRATUS_USER")
            :password (System/getenv "MIGRATUS_PASSWORD")
            :database (System/getenv "MIGRATUS_DATABASE")}}

     :db-access
     #::ds{:start
           (fn [opts] (new-db-access (get-in opts [::ds/config :db])))
           :config
           {:db (ds/ref [:storage :db])}}

     :group
     #::ds{:start
           (fn [opts] (group-repository/new-group-repository (get-in opts [::ds/config :db-access])))

           :config
           {:db-access (ds/ref [:storage :db-access])}}

     :user
     #::ds{:start
           (fn [opts] (db-user-repository/new-db-user-repository (get-in opts [::ds/config :db-access])))
           :config
           {:db-access (ds/ref [:storage :db-access])}}

     :file-storage
     #::ds{:start
           (fn [opts]
             {:content-response (partial file-storage/content-response (get-in opts [::ds/config]))
              :store-content (partial file-storage/store-content (get-in opts [::ds/config]))})
           :config
           {:storage-root "resources/uploads"
            :root-url (ds/ref [:env :galt-root-url])}}}
    :app
    {:reitit-middleware
     #::ds{:start
           (fn [opts]
             (let [galt-url (get-in opts [::ds/config :galt-url])
                   login-url (str galt-url "/members/login")]
               {:auth (partial middleware/wrap-auth login-url)}))
           :config {:galt-url (ds/ref [:env :galt-root-url])}}
     :route-handler
     #::ds{:start
           (fn [opts]
             (let [route-deps {:group-repo (get-in opts [::ds/config :group-repo])
                               :user-repo (get-in opts [::ds/config :user-repo])
                               :db-access (get-in opts [::ds/config :db-access])
                               :generate-name name-generator/generate
                               :file-storage (get-in opts [::ds/config :file-storage])
                               :reitit-middleware (get-in opts [::ds/config :reitit-middleware])
                               :galt-url (get-in opts [::ds/config :galt-url])}]
               (routes/handler (routes/router route-deps))))
           :config
           {:group-repo (ds/ref [:storage :group])
            :user-repo (ds/ref [:storage :user])
            :db-access (ds/ref [:storage :db-access])
            :file-storage (ds/ref [:storage :file-storage])
            :reitit-middleware (ds/ref [:app :reitit-middleware])
            :galt-url (ds/ref [:env :galt-root-url])
            }}
     :web-server
     #::ds{:start
           (fn [opts]
             (let [port (get-in opts [::ds/config :port])
                   handler (get-in opts [::ds/config :handler])]
               (web.server/start! handler {:port port :legacy-return-value? false})))
           :stop (fn [{:keys [::ds/instance]}]
                   (web.server/stop! instance))
           :config
           {:port (ds/ref [:env :web-server-port])
            :handler (ds/ref [:app :route-handler])}}}}})

(defmethod ds/named-system :base [_] system)

(defmethod ds/named-system :dev
  [_]
  (println ">>> starting :dev system")
  (ds/system :base {[:env] (get-config "config/dev-galt.edn")}))

(defmethod ds/named-system :prod
  [_]
  (println ">>> starting :prod system")
  (ds/system :base {[:env] (get-config "config/prod-galt.edn")}))

(defn start!
  [system-or-name]
  (if (keyword? system-or-name)
    (ds/signal (ds/system system-or-name) ::ds/start)
    (ds/signal system-or-name ::ds/start)))

(defn stop! [running-system]
  (ds/signal running-system ::ds/stop))

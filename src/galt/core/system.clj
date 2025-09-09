(ns galt.core.system
  (:require
    [donut.system :as ds]
    [galt.core.infrastructure.database :as database]
    [galt.core.infrastructure.web.middleware :as middleware]
    [galt.core.infrastructure.name-generator :as name-generator]
    [galt.core.views.layout :as layout] ; TODO see if can be removed
    [galt.core.infrastructure.web.helpers :as web-helpers] ; TODO see if can be removed
    [galt.core.infrastructure.web.routes :as core.routes]
    [galt.core.infrastructure.web.server :as web.server]
    [galt.members.external.routes]
    [galt.groups.external.routes]
    [galt.locations.external.routes]
    [galt.invitations.external.routes]
    [reitit.ring]
    [galt.core.infrastructure.disk-file-storage :as file-storage]
    [galt.groups.adapters.handlers]
    [galt.core.adapters.postgres-db-access :refer [new-db-access]]
    [galt.groups.adapters.db-group-repository :as group-repository]
    [galt.members.domain.user-repository :refer [find-user-by-id]]
    [galt.members.adapters.db-user-repository :as db-user-repository]
    [galt.members.adapters.db-member-repository :as db-member-repository]
    [galt.locations.adapters.pg-location-repository :as pg-location-repository]
    [galt.invitations.adapters.db-invitation-repository :refer [add-invitation-request user-invitation-requests]]
    [galt.invitations.use-cases.create-invitation :refer [create-invitation-use-case]]
    [clojure.java.io]
    [clj-uuid]
    [clojure.edn])
  (:import
   [com.zaxxer.hikari HikariDataSource]))


(defonce galt-session-atom (atom {}))

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

     :member
     #::ds{:start
           (fn [opts] (db-member-repository/new-db-member-repository (get-in opts [::ds/config :db-access])))
           :config
           {:db-access (ds/ref [:storage :db-access])}}

     :location
     #::ds{:start
           (fn [opts] (pg-location-repository/new-location-repository (get-in opts [::ds/config :db-access])))
           :config
           {:db-access (ds/ref [:storage :db-access])}}

     :file-storage
     #::ds{:start
           (fn [opts]
             {:content-response (partial file-storage/content-response (get-in opts [::ds/config]))
              :store-content (partial file-storage/store-content (get-in opts [::ds/config]))})
           :config
           {:storage-root (ds/ref [:env :file-storage-root])
            :root-url (ds/ref [:env :galt-root-url])}}
     :galt-session
     #::ds{:start
           (fn [_] galt-session-atom)}}

    :use-cases
    {:create-invitation
     #::ds{:start
           (fn [{{:keys [user-repo db]} ::ds/config}]
             (partial
               create-invitation-use-case
               {:find-user-by-id (partial find-user-by-id user-repo)
                :user-invitation-requests (partial user-invitation-requests db)
                :add-invitation-request (partial add-invitation-request db)}))
           :config
           {:db (ds/ref [:storage :db])
            :user-repo (ds/ref [:storage :user])
            :db-access (ds/ref [:storage :db-access])}}}
    :app
    {:reitit-middleware
     #::ds{:start
           (fn [opts]
             (let [galt-url (get-in opts [::ds/config :galt-url])
                   login-url (str galt-url "/members/login")]
               {:auth (partial middleware/wrap-auth login-url)}))
           :config {:galt-url (ds/ref [:env :galt-root-url])}}
     :route-deps
     #::ds{:start
           (fn [opts]
             (let [galt-session (get-in opts [::ds/config :galt-session])]
               {:app-container layout/app-container
                :render web-helpers/render-html
                :with-layout web-helpers/with-layout
                :galt-session galt-session
                :group-repo (get-in opts [::ds/config :group-repo])
                :user-repo (get-in opts [::ds/config :user-repo])
                :member-repo (get-in opts [::ds/config :member-repo])
                :location-repo (get-in opts [::ds/config :location-repo])
                :db-access (get-in opts [::ds/config :db-access])
                :generate-name name-generator/generate
                :file-storage (get-in opts [::ds/config :file-storage])
                :reitit-middleware (get-in opts [::ds/config :reitit-middleware])
                :galt-url (get-in opts [::ds/config :galt-url])
                :gen-uuid clj-uuid/v7}))
           :config
           {:galt-session (ds/ref [:storage :galt-session])
            :group-repo (ds/ref [:storage :group])
            :user-repo (ds/ref [:storage :user])
            :member-repo (ds/ref [:storage :member])
            :location-repo (ds/ref [:storage :location])
            :db-access (ds/ref [:storage :db-access])
            :file-storage (ds/ref [:storage :file-storage])
            :reitit-middleware (ds/ref [:app :reitit-middleware])
            :galt-url (ds/ref [:env :galt-root-url])
            }}
     :members/routes
     #::ds{:start
           (fn [{:keys [::ds/config]}]
             (galt.members.external.routes/router (:route-deps config)))
           :config
           {:route-deps (ds/ref [:app :route-deps])}}
     :groups/routes
     #::ds{:start
           (fn [{:keys [::ds/config]}]
             (galt.groups.external.routes/router (:route-deps config)))
           :config
           {:route-deps (ds/ref [:app :route-deps])}}
     :locations/routes
     #::ds{:start
           (fn [{:keys [::ds/config]}]
             (galt.locations.external.routes/router (:route-deps config)))
           :config
           {:route-deps (ds/ref [:app :route-deps])}}
     :invitations/routes
     #::ds{:start
           (fn [{:keys [::ds/config]}]
             (galt.invitations.external.routes/router (:route-deps config)))
           :config
           {:route-deps (ds/ref [:app :route-deps])}}
     :route-handler
     #::ds{:start
           (fn [{:keys [::ds/config]}]
             (let [members-router (get-in config [:members/routes])
                   groups-router (get-in config [:groups/routes])
                   locations-router (get-in config [:locations/routes])
                   invitations-router (get-in config [:invitations/routes])
                   route-deps (get-in config [:route-deps])
                   galt-session (get-in config [:galt-session])
                   core-router (core.routes/router route-deps)
                   final-router (core.routes/merge-routers
                                  members-router
                                  groups-router
                                  locations-router
                                  invitations-router
                                  core-router)]
               (core.routes/handler galt-session final-router)))
           :config
           {:members/routes (ds/ref [:app :members/routes])
            :groups/routes (ds/ref [:app :groups/routes])
            :locations/routes (ds/ref [:app :locations/routes])
            :invitations/routes (ds/ref [:app :invitations/routes])
            :route-deps (ds/ref [:app :route-deps])
            :galt-session (ds/ref [:storage :galt-session])}}
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

(defmethod ds/named-system :staging
  [_]
  (println ">>> starting :staging system")
  (ds/system :base {[:env] (get-config "config/staging-galt.edn")}))

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

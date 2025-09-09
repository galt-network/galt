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
    [galt.core.infrastructure.bitcoin.bouncy-castle-verify :refer [verify-signature]]
    [galt.members.external.routes]
    [galt.groups.external.routes]
    [galt.locations.external.routes]
    [galt.invitations.external.routes]
    [reitit.ring]
    [galt.core.infrastructure.disk-file-storage :as file-storage]
    [galt.groups.adapters.handlers]
    [galt.core.adapters.postgres-db-access :refer [new-db-access]]
    [galt.groups.adapters.db-group-repository :refer [new-group-repository]]

    [galt.members.domain.user-repository :as ur :refer [find-user-by-id]]
    [galt.members.domain.member-repository :as mr]
    [galt.groups.domain.group-repository :as gr]
    [galt.locations.domain.location-repository :as lr]

    [galt.members.adapters.db-user-repository :refer [new-db-user-repository]]
    [galt.members.adapters.db-member-repository :refer [new-db-member-repository]]
    [galt.locations.adapters.db-location-repository :refer [new-db-location-repository]]
    [galt.invitations.adapters.db-invitation-repository :refer [new-db-invitation-repository]]
    [galt.invitations.domain.invitation-repository :refer [add-invitation-request user-invitation-requests]]

    [galt.invitations.domain.use-cases.create-invitation :refer [create-invitation-use-case]]
    [galt.members.domain.use-cases.create-lightning-user :refer [create-lightning-user-use-case]]
    [galt.groups.domain.use-cases.update-group :refer [update-group-use-case]]
    [galt.groups.domain.use-cases.delete-group :refer [delete-group-use-case]]
    [galt.groups.domain.use-cases.add-group :refer [add-group-use-case]]
    [galt.groups.domain.use-cases.edit-group :refer [edit-group-use-case]]

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
           (fn [{:keys [::ds/config]}] (new-db-access (config :db)))
           :config
           {:db (ds/ref [:storage :db])}}

     :group
     #::ds{:start
           (fn [{:keys [::ds/config]}] (new-group-repository (config :db-access)))

           :config
           {:db-access (ds/ref [:storage :db-access])}}

     :user
     #::ds{:start
           (fn [{:keys [::ds/config]}] (new-db-user-repository (config :db-access)))
           :config
           {:db-access (ds/ref [:storage :db-access])}}

     :member
     #::ds{:start
           (fn [{:keys [::ds/config]}] (new-db-member-repository (config :db-access)))
           :config
           {:db-access (ds/ref [:storage :db-access])}}

     :location
     #::ds{:start
           (fn [{:keys [::ds/config]}] (new-db-location-repository (config :db-access)))
           :config
           {:db-access (ds/ref [:storage :db-access])}}

     :invitation
     #::ds{:start
           (fn [{:keys [::ds/config]}] (new-db-invitation-repository (:db-access config)))
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
    {:create-invitation-use-case
     #::ds{:start
           (fn [{{:keys [user-repo invitation-repo]} ::ds/config}]
             (partial
               create-invitation-use-case
               {:find-user-by-id (partial find-user-by-id user-repo)
                :user-invitation-requests (partial user-invitation-requests invitation-repo)
                :add-invitation-request (partial add-invitation-request invitation-repo)}))
           :config
           {:db (ds/ref [:storage :db])
            :user-repo (ds/ref [:storage :user])
            :invitation-repo (ds/ref [:storage :invitation])}}

     :create-lightning-user-use-case
     #::ds{:start
           (fn [{{:keys [user-repo member-repo]} ::ds/config}]
             (partial create-lightning-user-use-case
                      {:verify-signature verify-signature
                       :gen-uuid clj-uuid/v7
                       :find-user-by-pub-key (partial ur/find-user-by-pub-key user-repo)
                       :find-member-by-user-id (partial mr/find-member-by-user-id member-repo)
                       :add-user (partial ur/add-user user-repo)}))
           :config
           {:user-repo (ds/ref [:storage :user])
            :member-repo (ds/ref [:storage :member])}}

     :add-group-use-case
     #::ds{:start
           (fn [{{:keys [group-repo location-repo]} ::ds/config}]
             (partial add-group-use-case
                      {:find-group-by-id (partial gr/find-group-by-id group-repo)
                       :find-groups-by-founder-id (partial gr/find-groups-by-founder-id group-repo)
                       :find-groups-by-name (partial gr/find-groups-by-name group-repo)
                       :add-group (partial gr/add-group group-repo)
                       :add-location (partial lr/add-location location-repo)
                       :gen-uuid clj-uuid/v7
                       }))
           :config
           {:group-repo (ds/ref [:storage :group])
            :location-repo (ds/ref [:storage :location])}}

     :update-group-use-case
     #::ds{:start
           (fn [{{:keys [group-repo]} ::ds/config}]
             (partial update-group-use-case
                      {:find-group-by-id (partial gr/find-group-by-id group-repo)}))
           :config
           {:group-repo (ds/ref [:storage :group])}}

     :delete-group-use-case
     #::ds{:start
           (fn [{{:keys [group-repo]} ::ds/config}]
             (partial delete-group-use-case
                      {:delete-group (partial gr/delete-group group-repo)}))
           :config
           {:group-repo (ds/ref [:storage :group])}}

     :edit-group-use-case
     #::ds{:start
           (fn [{{:keys [group-repo]} ::ds/config}]
             (partial edit-group-use-case
                      {:find-group-by-id (partial gr/find-group-by-id group-repo)}))
           :config
           {:group-repo (ds/ref [:storage :group])}}
     }

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
           (fn [{:keys [::ds/config]}]
             (let [galt-session (config :galt-session)]
               (merge
                 {:app-container layout/app-container
                  :render web-helpers/render-html
                  :with-layout web-helpers/with-layout
                  :galt-session galt-session
                  :group-repo (config :group-repo)
                  :user-repo (config :user-repo)
                  :member-repo (config :member-repo)
                  :location-repo (config :location-repo)
                  :db-access (config :db-access)
                  :generate-name name-generator/generate
                  :file-storage (config :file-storage)
                  :reitit-middleware (config :reitit-middleware)
                  :galt-url (config :galt-url)
                  :gen-uuid clj-uuid/v7}
                 (config :use-cases))))
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
            :use-cases (ds/ref [:use-cases])
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

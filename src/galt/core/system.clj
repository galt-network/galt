(ns galt.core.system
  (:require
   [clj-uuid]
   [clojure.edn]
   [clojure.java.io]
   [donut.system :as ds]
   [galt.core.adapters.db-access]
   [galt.core.adapters.postgres-db-access :refer [new-db-access]]
   [galt.core.infrastructure.bitcoin.bouncy-castle-verify :refer [verify-signature]]
   [galt.core.infrastructure.bitcoin.lnurl :refer [generate-lnurl]]
   [galt.core.infrastructure.database :as database]
   [galt.core.infrastructure.disk-file-storage :as file-storage]
   [galt.core.infrastructure.name-generator :as name-generator]
   [galt.core.infrastructure.web.helpers :as web-helpers] ; TODO see if can be removed
   [galt.core.infrastructure.web.routes :as core.routes]
   [galt.core.infrastructure.web.server :as web.server]
   [galt.core.views.layout :as layout] ; TODO see if can be removed
   [galt.groups.adapters.db-group-repository :refer [new-group-repository]]
   [galt.groups.adapters.handlers]
   [galt.groups.domain.group-repository :as gr]
   [galt.groups.domain.use-cases.add-group :refer [add-group-use-case]]
   [galt.groups.domain.use-cases.delete-group :refer [delete-group-use-case]]
   [galt.groups.domain.use-cases.edit-group :refer [edit-group-use-case]]
   [galt.groups.domain.use-cases.new-group :refer [new-group-use-case]]
   [galt.groups.domain.use-cases.update-group :refer [update-group-use-case]]
   [galt.groups.external.routes]
   [galt.invitations.adapters.db-invitation-repository :refer [new-db-invitation-repository]]
   [galt.invitations.domain.invitation-repository :as ir]
   [galt.invitations.domain.use-cases.create-invitation :refer [create-invitation-use-case]]
   [galt.invitations.domain.use-cases.create-invitation-request :refer [create-invitation-request-use-case]]
   [galt.invitations.domain.use-cases.invitation-dashboard :refer [invitation-dashboard-use-case]]
   [galt.invitations.external.routes]
   [galt.locations.adapters.db-location-repository :refer [new-db-location-repository]]
   [galt.locations.domain.location-repository :as lr]
   [galt.locations.external.routes]
   [galt.members.adapters.db-member-repository :refer [new-db-member-repository]]
   [galt.members.adapters.db-user-repository :refer [new-db-user-repository]]
   [galt.members.domain.member-repository :as mr]
   [galt.payments.adapters.db-payment-repository :refer [new-db-payment-repository]]
   [galt.payments.domain.payment-repository :as pr]
   [galt.members.domain.use-cases.complete-lnurl-login :refer [complete-lnurl-login-use-case]]
   [galt.members.domain.use-cases.create-lightning-user :refer [create-lightning-user-use-case]]
   [galt.members.domain.use-cases.search-members :refer [search-members-use-case]]
   [galt.members.domain.use-cases.show-profile :refer [show-profile-use-case]]
   [galt.members.domain.use-cases.start-lnurl-login :refer [start-lnurl-login-use-case]]
   [galt.members.domain.use-cases.watch-lnurl-login :refer [watch-lnurl-login-use-case]]
   [galt.members.domain.user-repository :as ur :refer [find-user-by-id]]
   [galt.members.external.routes]
   [galt.payments.adapters.cln-payment-gateway :refer [new-cln-payment-gateway]]
   [galt.payments.domain.payment-gateway :as pg]
   [galt.payments.domain.use-cases.membership-payment :refer [membership-payment-use-case]]
   [galt.payments.domain.use-cases.update-invoice :refer [update-invoice-use-case]]
   [galt.payments.external.routes]
   [reitit.ring]
   [ring.middleware.session.memory :as memory]
   [ring.middleware.session.store :refer [delete-session read-session
                                          write-session]])
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

(defonce galt-session-atom (atom {}))

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

     :payment
     #::ds{:start
           (fn [{:keys [::ds/config]}] (new-db-payment-repository (:db-access config)))
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
           (fn [_] galt-session-atom )}

     :session-store
     #::ds{:start
           (fn [{:keys [::ds/config]}] (memory/memory-store (:storage config)))
           :config
           {:storage (ds/ref [:storage :galt-session])}}

     :session-protocol-methods
     #::ds{:start
           (fn [{{:keys [session-store]} ::ds/config}]
             {:read-session (partial read-session session-store)
              :write-session (partial write-session session-store)
              :delete-session (partial delete-session session-store)})
           :config
           {:session-store (ds/ref [:storage :session-store])}}}

    :gateways
    {:payment
     #::ds{:start
           (fn [{{:keys [node-url rune]} ::ds/config}]
             (println ">>> Configuring ClnPaymentGateway with" node-url rune)
             (new-cln-payment-gateway node-url rune))
           :config
           {:node-url (ds/ref [:env :cln-rest-url])
            :rune (ds/ref [:env :cln-rune])}}}

    :use-cases
    {:create-invitation-request-use-case
     #::ds{:start
           (fn [{{:keys [user-repo invitation-repo]} ::ds/config}]
             (partial
               create-invitation-request-use-case
               {:find-user-by-id (partial find-user-by-id user-repo)
                :user-invitation-requests (partial ir/user-invitation-requests invitation-repo)
                :add-invitation-request (partial ir/add-invitation-request invitation-repo)}))
           :config
           {:user-repo (ds/ref [:storage :user])
            :invitation-repo (ds/ref [:storage :invitation])}}

     :invitation-dashboard-use-case
     #::ds{:start
           (fn [{{:keys [db group-repo]} ::ds/config}]
             (partial
               invitation-dashboard-use-case
               {:query (partial galt.core.adapters.postgres-db-access/query db)
                :find-groups-by-member (partial gr/find-groups-by-member group-repo)
                :pub-key->name name-generator/generate}))
           :config
           {:db (ds/ref [:storage :db])
            :group-repo (ds/ref [:storage :group])}}

     :create-invitation-use-case
     #::ds{:start
           (fn [{{:keys [invitation-repo]} ::ds/config}]
             (partial
               create-invitation-use-case
               {:add-invitation (partial ir/add-invitation invitation-repo)}))
           :config
           {:invitation-repo (ds/ref [:storage :invitation])}}

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
           (fn [{{:keys [group-repo location-repo member-repo]} ::ds/config}]
             (partial add-group-use-case
                      {:find-group-by-id (partial gr/find-group-by-id group-repo)
                       :find-member-by-user-id (partial mr/find-member-by-user-id member-repo)
                       :find-groups-by-founder-id (partial gr/find-groups-by-founder-id group-repo)
                       :find-groups-by-name (partial gr/find-groups-by-name group-repo)
                       :add-group (partial gr/add-group group-repo)
                       :add-location (partial lr/add-location location-repo)
                       :gen-uuid clj-uuid/v7
                       }))
           :config
           {:group-repo (ds/ref [:storage :group])
            :member-repo (ds/ref [:storage :member])
            :location-repo (ds/ref [:storage :location])}}

     :new-group-use-case
     #::ds{:start
           (fn [{{:keys [member-repo]} ::ds/config}]
             (partial new-group-use-case
                      {:find-member-by-user-id (partial mr/find-member-by-user-id member-repo)}))
           :config
           {:member-repo (ds/ref [:storage :member])}}

     :show-profile-use-case
     #::ds{:start
           (fn [{{:keys [member-repo group-repo location-repo]} ::ds/config}]
             (partial show-profile-use-case
                      {:find-member-by-id (partial mr/find-member-by-id member-repo)
                       :find-groups-by-member (partial gr/find-groups-by-member group-repo)
                       :find-location-by-id (partial lr/find-location-by-id location-repo)}))
           :config
           {:member-repo (ds/ref [:storage :member])
            :group-repo (ds/ref [:storage :group])
            :location-repo (ds/ref [:storage :location])}}

     :search-members-use-case
     #::ds{:start
           (fn [{{:keys [member-repo group-repo]} ::ds/config}]
             (partial search-members-use-case
                      {:find-members-by-name (partial mr/find-members-by-name member-repo)
                       :list-members (partial mr/list-members member-repo)
                       :find-groups-by-member (partial gr/find-groups-by-member group-repo)}))
           :config
           {:member-repo (ds/ref [:storage :member])
            :group-repo (ds/ref [:storage :group])}}

     :start-lnurl-login-use-case
     #::ds{:start
           (fn [{:keys [::ds/config]}]
             (partial start-lnurl-login-use-case
                      {:generate-lnurl (partial generate-lnurl (:galt-root-url config))
                       :session-store (:session-store config)}))
           :config
           {:galt-root-url (ds/ref [:env :galt-root-url])
            :session-store (ds/ref [:storage :session-store])}}

     :complete-lnurl-login-use-case
     #::ds{:start
           (fn [{{:keys [session-store user-repo member-repo]} ::ds/config}]
             (partial complete-lnurl-login-use-case
                      {:session-store session-store
                       :verify-signature verify-signature
                       :gen-uuid clj-uuid/v7
                       :find-user-by-pub-key (partial ur/find-user-by-pub-key user-repo)
                       :find-member-by-user-id (partial mr/find-member-by-user-id member-repo)
                       :add-user (partial ur/add-user user-repo)}))
           :config
           {:session-store (ds/ref [:storage :session-store])
            :user-repo (ds/ref [:storage :user])
            :member-repo (ds/ref [:storage :member])}}

     :watch-lnurl-login-use-case
     #::ds{:start
           (fn [{{:keys [session-methods]} ::ds/config}]
             (partial watch-lnurl-login-use-case (select-keys session-methods [:read-session])))
           :config
           {:session-methods (ds/ref [:storage :session-protocol-methods])}}

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
                      {:find-group-by-id (partial gr/find-group-by-id group-repo)
                       :find-membership-by-member (partial gr/find-membership-by-member group-repo)}))
           :config
           {:group-repo (ds/ref [:storage :group])}}

     :membership-payment-use-case
     #::ds{:start
           (fn [{{:keys [payment-repo payment-gw]} ::ds/config}]
             (partial membership-payment-use-case
                      {:membership-invoices (partial pr/membership-invoices payment-repo)
                       :add-membership-invoice (partial pr/add-membership-invoice payment-repo)
                       :create-invoice (partial pg/create-invoice payment-gw)}))
           :config
           {:member-repo (ds/ref [:storage :member])
            :payment-repo (ds/ref [:storage :payment])
            :payment-gw (ds/ref [:gateways :payment])}}

     :update-invoice-use-case
     #::ds{:start
           (fn [{{:keys [payment-repo payment-gw]} ::ds/config}]
             (partial update-invoice-use-case
                      {:db-invoice-by-label (partial pr/invoice-by-label payment-repo)
                       :ln-invoice-by-label (partial pg/invoice-by-label payment-gw)
                       :update-membership-invoice (partial pr/update-membership-invoice payment-repo)}))
           :config
           {:payment-repo (ds/ref [:storage :payment])
            :payment-gw (ds/ref [:gateways :payment])}}}

    :app
    {:route-deps
     #::ds{:start
           (fn [{:keys [::ds/config]}]
             (merge
               {:app-container layout/app-container
                :render web-helpers/render-html
                :with-layout web-helpers/with-layout
                :group-repo (config :group-repo)
                :user-repo (config :user-repo)
                :member-repo (config :member-repo)
                :location-repo (config :location-repo)
                :invitation-repo (config :invitation-repo)
                :db-access (config :db-access)
                :generate-name name-generator/generate
                :file-storage (config :file-storage)
                :galt-url (config :galt-url)
                :gen-uuid clj-uuid/v7
                :read-session (partial read-session (:session-store config))
                :write-session (partial write-session (:session-store config))
                :delete-session (partial delete-session (:session-store config))}
               (config :use-cases)))
           :config
           {:group-repo (ds/ref [:storage :group])
            :user-repo (ds/ref [:storage :user])
            :member-repo (ds/ref [:storage :member])
            :location-repo (ds/ref [:storage :location])
            :invitation-repo (ds/ref [:storage :invitation])
            :db-access (ds/ref [:storage :db-access])
            :file-storage (ds/ref [:storage :file-storage])
            :galt-url (ds/ref [:env :galt-root-url])
            :session-store (ds/ref [:storage :session-store])
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
     :payments/routes
     #::ds{:start
           (fn [{:keys [::ds/config]}]
             (galt.payments.external.routes/router (:route-deps config)))
           :config
           {:route-deps (ds/ref [:app :route-deps])}}
     :router
     #::ds{:start
           (fn [{:keys [::ds/config]}]
             (let [members-router (get-in config [:members/routes])
                   groups-router (get-in config [:groups/routes])
                   locations-router (get-in config [:locations/routes])
                   invitations-router (get-in config [:invitations/routes])
                   payments-router (get-in config [:payments/routes])
                   route-deps (get-in config [:route-deps])
                   core-router (core.routes/router route-deps)]
               (core.routes/merge-routers
                 members-router
                 groups-router
                 locations-router
                 invitations-router
                 payments-router
                 core-router)))
           :config
           {:members/routes (ds/ref [:app :members/routes])
            :groups/routes (ds/ref [:app :groups/routes])
            :locations/routes (ds/ref [:app :locations/routes])
            :invitations/routes (ds/ref [:app :invitations/routes])
            :payments/routes (ds/ref [:app :payments/routes])
            :route-deps (ds/ref [:app :route-deps])}}
     :route-handler
     #::ds{:start
           (fn [{:keys [::ds/config]}]
             (core.routes/handler (:session-store config) (:router config)))
           :config
           {:session-store (ds/ref [:storage :session-store])
            :router (ds/ref [:app :router])}}

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

(ns galt.world-map.adapters.handlers
  (:require
   [galt.core.adapters.view-models :as core-view-models]
   [galt.core.infrastructure.web.helpers :as web-helpers]
   [galt.core.views.layout :as layout]
   [galt.members.domain.member-repository :as mr]
   [galt.members.domain.user-repository :as ur]
   [galt.world-map.adapters.placeables :as placeables]
   [galt.world-map.adapters.presentation :as presentation]
   [is.galt.globo.protocols :as protocols]
   [taoensso.telemere :as tel]))

(defn wrap-globo-user-id
  "Sets the globo user-id on the request from the GALT session:
  member-id, else user-id, else a persistent anonymous id seeded into the
  session so the outer wrap-session middleware persists it."
  [handler]
  (fn [req]
    (let [session (:session req)
          anon-id (or (:globo-anon-id session) (str "anon-" (random-uuid)))
          user-id (str (or (:member-id session) (:user-id session) anon-id))
          seeded? (nil? (:globo-anon-id session))
          req' (cond-> (assoc req :user-id user-id)
                 seeded? (assoc :session (assoc session :globo-anon-id anon-id)))
          resp (handler req')]
      (if seeded?
        (update resp :session merge (:session req'))
        resp))))

(defn galt-user-name
  "Display name for a GALT user-id: the member profile name when one
   exists, else the generated user name. The user-id arrives as a string
   (globo key form); it is parsed back to a UUID for the repository
   lookups, since the DB id columns are uuid-typed."
  [{:keys [user-repo member-repo]} user-id]
  (let [user-id (try (java.util.UUID/fromString user-id) (catch Exception _ nil))]
    (or (some->> user-id (mr/find-member-by-id member-repo) :name)
        (some->> user-id (ur/find-user-by-id user-repo) :name))))

(defn wrap-globo-user-name
  "Seeds the user's GALT display name into globo storage so the
   online-users list and chat messages show real names. Anonymous users
   are left without a name. Runs after wrap-globo-user-id (so :user-id is
   set); the connection handler's register-user! then completes the user
   map while preserving the seeded :name."
  [deps handler]
  (fn [req]
    (let [user-id (:user-id req)]
      (when-not (placeables/anon-user-id? user-id)
        (when-let [name (galt-user-name deps user-id)]
          (when (seq name)
            (protocols/update-user! (-> deps :globo :storage) user-id
                                    #(assoc % :name name)))))
      (handler req))))

(defn wrap-error-logging
  "Logs uncaught exceptions with a stack trace and returns a plain-text 500
   response, replacing globo's silent wrap-error-response."
  [handler]
  (fn [req]
    (try
      (handler req)
      (catch Exception ex
        (tel/log! {:level :error
                   :msg (str "Exception handling " (name (:request-method req)) " " (:uri req))
                   :data {:error ex}})
        {:status 500
         :headers {"Content-Type" "text/plain"}
         :body (str "Oops, something went wrong\n" (.getMessage ex))}))))

(defn with-world-map-layout
  "Like web-helpers/with-layout but renders the world-map layout."
  [deps handler]
  (fn [req]
    (let [mount-path (or (:globo-mount-path deps) "/world-map")
          layout-model (core-view-models/layout-model deps req)]
      (handler (merge deps
                      {:layout (partial web-helpers/layout-for-content
                                        (partial presentation/world-map-layout mount-path)
                                        layout-model)
                       :navbar layout/navbar
                       :content layout/content
                       :update-layout-model (fn [req] (core-view-models/layout-model deps req))
                       :layout-model layout-model}) req))))

(defn world-map-page
  "Renders the full-bleed world map page."
  [{:keys [layout render]} _req]
  {:status 200
   :body (render (layout {:content [:div#app]}))})

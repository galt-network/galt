(ns galt.world-map.adapters.handlers
  (:require
   [galt.core.adapters.view-models :as core-view-models]
   [galt.core.infrastructure.web.helpers :as web-helpers]
   [galt.core.views.layout :as layout]
   [galt.world-map.adapters.presentation :as presentation]))

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
        (update resp :session merge {:globo-anon-id anon-id})
        resp))))

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

(ns galt.core.infrastructure.web.routes
  (:require
    [galt.core.infrastructure.web.middleware :as middleware]
    [galt.core.adapters.handlers :as core-handlers]
    [reitit.core]
    [reitit.ring :as rr]
    [ring.middleware.cors :refer [wrap-cors]]
    [ring.middleware.content-type :refer [wrap-content-type]]
    [ring.middleware.session :refer [wrap-session]]
    [ring.middleware.keyword-params :refer [wrap-keyword-params]]
    [ring.middleware.params :refer [wrap-params]]
    [ring.middleware.multipart-params :refer [wrap-multipart-params]]
    ))

(defn router
 [deps]
 (let [with-layout (:with-layout deps)
       with-deps-layout (partial with-layout deps)]
   (rr/router
     [["/" {:id :home
            :get (with-deps-layout core-handlers/view-landing)}]
      ["/datastar-sse" {:name :datastar-sse
                        :post (partial core-handlers/datastar-sse deps)}]
      ["/files" {:post (partial core-handlers/store-file deps)}]
      ["/files/*path" {:get (partial core-handlers/serve-file deps)}]
      ["/assets/*" {:name :assets
                    :handler (-> (rr/create-resource-handler)
                                 (wrap-cors ,,,
                                            :access-control-allow-origin #".*"
                                            :access-control-allow-methods [:get])
                                 (wrap-content-type ,,, {:mime-types {"cljs" "application/x-scittle"}}))}]])))

(defn handler [session-store router]
  (-> (rr/ring-handler router nil)
      (middleware/wrap-auth ,,, router)
      middleware/wrap-with-logger
      middleware/wrap-method-override
      wrap-keyword-params
      wrap-params
      wrap-multipart-params
      (middleware/wrap-update-related-session ,,, session-store)
      (wrap-session ,,, {:store session-store})))

(defn merge-routers [& routers]
  (reitit.core/router
     (apply merge (map reitit.core/routes routers))
     (apply merge (map reitit.core/options routers))))

(ns galt.core.infrastructure.web.middleware
  (:require
    [starfederation.datastar.clojure.api :as d*]
    [galt.core.adapters.sse-helpers :refer [with-sse]]
    [taoensso.telemere :as tel]
    [clojure.string :as str]
    ))

(defn wrap-method-override
  "Adds support for PUT, DELETE and PATCH methods via _method POST(<form>) parameter"
  [handler]
  (fn [request]
    (if (= :post (:request-method request))
      (let [override-method (get-in request [:params :_method])]
        (if (and override-method (#{"PUT" "DELETE" "PATCH"} (str/upper-case override-method)))
          (handler (assoc request :request-method (keyword (str/lower-case override-method))))
          (handler request)))
      (handler request))))

(defn wrap-auth
  [login-url handler]
  (fn [request]
    (if (get-in request [:session :user-id])
      (handler request)
      (if (d*/datastar-request? request)
        (with-sse request (fn [send!] (send! :js (str "window.location.href = '" login-url "'"))))
        {:status 302 :headers {"Location" login-url}}))))

(defn wrap-add-galt-session
  [handler session-atom]
  (fn [req]
    (let [res (handler req)
          [session-id props] (get res :update-related-session)]
      (when session-id
        (reset! session-atom (assoc-in @session-atom
                                       [session-id]
                                       (merge (get-in @session-atom [session-id]) props))))
      res)))

(defn- duration-from
  [start-timestamp]
  (- (System/currentTimeMillis) start-timestamp))

(defn- log-data [request start-timestamp final-data]
  (let [route-path (get-in request [:uri])
        session-id (get-in request [:session/key])
        user-id (get-in request [:session :user-id])
        request-method (-> request
                           (get ,,, :request-method)
                           name
                           str/upper-case)
        request-info (str request-method " " route-path " from " (:remote-addr request))
        message request-info
        updated-log-level (if (get-in final-data [:data :error])
                            {:level :warn}
                            {})]
    (merge-with
      merge
      {:level :info
       :msg message
       :data {:duration (duration-from start-timestamp)
              :path route-path
              :session-id session-id
              :user-id user-id}}
      final-data
      updated-log-level)))

; Info about logged out namespace & line number
;  https://cljdoc.org/d/com.taoensso/telemere/1.1.0/api/taoensso.telemere#help:signal-content
(def last-response (atom nil))

(defn wrap-with-logger
  [handler]
   (fn [request]
     (let [start-timestamp (System/currentTimeMillis)
           finalize-log-data (partial log-data request start-timestamp)]
       (try
         (let [response (handler request)]
           (reset! last-response response)
           (tel/log! (finalize-log-data {:data {:status (:status response)}}))
           response)
         (catch Exception ex
           (tel/log! (finalize-log-data {:data {:error ex}}))
           (throw ex))))))

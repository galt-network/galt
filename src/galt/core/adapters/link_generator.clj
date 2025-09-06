(ns galt.core.adapters.link-generator
  (:require
    [reitit.core]))

(defn link-for-route
  [router-or-req route-name & [path-params]]
  (let [router (if (reitit.core/router? router-or-req)
                 router-or-req
                 (get router-or-req :reitit.core/router))]
    (when (nil? router) (throw (AssertionError. "Couldn't get reitit router")))
    (:path (reitit.core/match-by-name router route-name (or path-params nil)))))

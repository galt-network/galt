(ns galt.locations.adapters.geoip
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [galt.locations.adapters.client-ip :as client-ip])
  (:import
   (java.net InetAddress)))

(def default-radius-km 100)

(defonce ^:private readers (atom {}))

(defn- reader
  [path]
  (or (get @readers path)
      (let [builder-class (Class/forName "com.maxmind.geoip2.DatabaseReader$Builder")
            ctor (.getConstructor builder-class (into-array Class [java.io.File]))
            builder (.newInstance ctor (into-array Object [(io/file path)]))
            r (.build builder)]
        (swap! readers assoc path r)
        r)))

(defn lookup
  [path ip]
  (when (and path ip (.exists (io/file path)))
    (try
      (let [resp (.city (reader path) (InetAddress/getByName ip))
            loc (.getLocation resp)
            city (some-> resp .getCity .getName)
            country (some-> resp .getCountry .getName)
            lat (.getLatitude loc)
            lng (.getLongitude loc)]
        (when (and lat lng)
          {:lat lat
           :lng lng
           :label (str/join ", " (remove str/blank? [city country]))
           :radius-km default-radius-km}))
      (catch Exception _ nil))))

(defn infer-from-request
  [path req]
  (lookup path (client-ip/client-ip req)))

(defn- member-location
  [{:keys [find-member find-location]} req]
  (when (and find-member find-location)
    (when-let [member-id (get-in req [:session :member-id])]
      (when-let [loc (some-> (find-member member-id) :location-id find-location)]
        (when (and (:latitude loc) (:longitude loc))
          {:lat (:latitude loc)
           :lng (:longitude loc)
           :label (:name loc)
           :radius-km default-radius-km})))))

(defn make-infer-location
  [{:keys [mmdb-path] :as deps}]
  (fn [req]
    (or (member-location deps req)
        (infer-from-request mmdb-path req))))

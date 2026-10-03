(ns galt.locations.adapters.client-ip
  (:require
   [clojure.string :as str])
  (:import
   (java.net InetAddress URI)
   (java.net.http HttpClient HttpRequest HttpResponse$BodyHandlers)
   (java.time Duration)))

(defonce ^:private public-ip-cache (atom nil))

(defn clear-public-ip-cache!
  []
  (reset! public-ip-cache nil))

(defn- header
  [req name]
  (let [headers (:headers req)]
    (or (get headers name)
        (get headers (str/lower-case name)))))

(defn- loopback?
  [ip]
  (boolean
   (when (and ip (not (str/blank? ip)))
     (try
       (.isLoopbackAddress (InetAddress/getByName ip))
       (catch Exception _ false)))))

(defn fetch-public-ip
  []
  (try
    (let [client (-> (HttpClient/newBuilder)
                     (.connectTimeout (Duration/ofSeconds 2))
                     .build)
          request (-> (HttpRequest/newBuilder (URI/create "https://api.ipify.org"))
                      (.timeout (Duration/ofSeconds 2))
                      .GET
                      .build)
          response (.send client request (HttpResponse$BodyHandlers/ofString))
          body (some-> (.body response) str/trim not-empty)]
      (when (= 200 (.statusCode response))
        body))
    (catch Exception _ nil)))

(defn- public-ip
  []
  (or @public-ip-cache
      (when-let [ip (fetch-public-ip)]
        (reset! public-ip-cache ip)
        ip)))

(defn client-ip
  [req]
  (let [ip (or (header req "cf-connecting-ip")
               (header req "true-client-ip")
               (header req "x-real-ip")
               (some-> (header req "x-forwarded-for")
                       (str/split #",")
                       first
                       str/trim
                       not-empty)
               (:remote-addr req))]
    (if (loopback? ip)
      (public-ip)
      ip)))

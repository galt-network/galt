(ns galt-promo.server
  (:require [clojure.string :as str]
            [galt-promo.names :as names]
            [galt-promo.presence :as presence]
            [macchiato.middleware.resource :as resource]
            [macchiato.server :as http]
            [macchiato.util.response :as response]
            [reitit.ring :as ring]))

(defn- browser-name [user-agent]
  (cond
    (str/includes? user-agent "Edg/") "Edge"
    (str/includes? user-agent "Chrome/") "Chrome"
    (str/includes? user-agent "Firefox/") "Firefox"
    (str/includes? user-agent "Safari/") "Safari"
    :else "Other"))

(defn- snapshot [registry]
  (->> @registry
       vals
       (map #(dissoc % :response :disconnected-at))
       (sort-by :name)
       vec))

(defn- broadcast! [registry]
  (let [message (str "event: users\ndata: "
                     (js/JSON.stringify (clj->js (snapshot registry)))
                     "\n\n")]
    (doseq [{:keys [response]} (vals @registry)
            :when response]
      (.write response message))))

(defn- connect [registry {:keys [headers node/request node/response query-string remote-addr uri]} respond _]
  (let [url (js/URL. (str "http://localhost" uri "?" query-string))
        client-id (.get (.-searchParams url) "client-id")]
    (if (str/blank? client-id)
      (respond {:status 400 :body "client-id is required"})
      (let [taken (into #{} (map :name) (vals @registry))]
        (swap! registry assoc client-id
               {:id client-id
                :name (or (get-in @registry [client-id :name])
                          (names/generate-name taken rand))
                :browser (browser-name (get headers "user-agent" ""))
                :ip (str/replace remote-addr #"^::ffff:" "")
                :connected? true
                :disconnected-at nil
                :response response})
        (.writeHead ^js response 200 #js {"Content-Type" "text/event-stream"
                                      "Cache-Control" "no-cache"
                                      "Connection" "keep-alive"})
        (.write ^js response "retry: 3000\n")
        (broadcast! registry)
        (.once request "close"
               (fn []
                 (swap! registry
                        (fn [users]
                          (if (identical? response (get-in users [client-id :response]))
                            (-> users
                                (assoc-in [client-id :connected?] false)
                                (assoc-in [client-id :disconnected-at] (js/Date.now))
                                (assoc-in [client-id :response] nil))
                            users)))
                 (broadcast! registry)))))))

(defn make-handler [registry]
  (resource/wrap-resource
   (ring/ring-handler
    (ring/router [["/connect" {:get (partial connect registry)}]
                  ["/" {:get (fn [_ respond _]
                               (respond (response/file "resources/public/index.html")))}]])
    (fn [_ respond _]
      (respond {:status 404 :body "Not found"})))
   "resources/public"))

(defn- maintain! [registry]
  (doto (js/setInterval
         (fn []
           (let [users (presence/prune-disconnected @registry (js/Date.now))]
             (when (not= users @registry)
               (reset! registry users)
               (broadcast! registry)))
           (doseq [{:keys [response]} (vals @registry) :when response]
             (.write ^js response ": keepalive\n\n")))
         1000)
    (.unref)))

(defn main [& _]
  (let [port (js/parseInt (or (.. js/process -env -PORT) "3000") 10)
        registry (atom {})]
    (maintain! registry)
    (http/start {:handler (make-handler registry)
                 :host "0.0.0.0"
                 :port port
                 :on-success #(println (str "Listening on http://localhost:" port))})))

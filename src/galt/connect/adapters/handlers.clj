(ns galt.connect.adapters.handlers
  (:require
   [clojure.string :as str]
   [galt.connect.adapters.presentation.connect :as presentation]
   [galt.connect.domain.use-cases.search :as search]
   [galt.core.adapters.sse-helpers :refer [with-sse]]
   [galt.core.adapters.time-helpers :as th]
   [galt.core.infrastructure.web.helpers :refer [get-signals]]
   [ring.util.http-status :as http-status]
   [starfederation.datastar.clojure.api :refer [datastar-request?]]))

(defn- parse-types
  [raw]
  (when (and raw (not (and (string? raw) (str/blank? raw))))
    (->> (if (string? raw) (str/split raw #",") raw)
         (map keyword)
         set)))

(def ^:private type-flags [:member :group :event :post])

(defn- types-from-signals
  [signals]
  (when (some #(contains? signals %) type-flags)
    (->> type-flags
         (filter #(true? (get signals %)))
         set)))

(defn- location-from-params
  [qp params signals]
  (let [lat (some-> (or (get qp "lat") (get params :lat) (:lat signals)) str parse-double)
        lng (some-> (or (get qp "lng") (get params :lng) (:lng signals)) str parse-double)
        label (or (get qp "label") (get params :label) (:label signals))
        radius (or (some-> (or (get qp "radius") (get params :radius) (:radius signals)) str parse-double) 100)]
    (when (and lat lng)
      {:lat lat :lng lng :label label :radius-km radius})))

(defn- geo-off?
  [qp params signals]
  (= "off" (or (get qp "geo") (get params :geo) (:geo signals))))

(defn- resolve-location
  [deps req signals]
  (let [qp (:query-params req)
        params (:params req)]
    (when-not (geo-off? qp params signals)
      (or (location-from-params qp params signals)
          (when-let [infer (:infer-location deps)]
            (infer req))))))

(defn- command-from
  [deps req]
  (let [qp (:query-params req)
        params (:params req)
        signals (when (datastar-request? req) (get-signals req))
        period (or (get qp "period") (get params :period) (:period signals))
        [from to] (when (and period (not= period "all"))
                    (th/period-range (keyword period)))
        location (resolve-location deps req signals)
        patch-mode (or (get qp "patch-mode") (get params :patch-mode))
        limit (or (some-> (or (get qp "limit") (get params :limit) (:limit signals)) str parse-long) 10)
        offset (if (= "inner" patch-mode)
                 0
                 (or (some-> (or (get qp "offset") (get params :offset) (:offset signals)) str parse-long) 0))]
    (cond-> {:query (or (get qp "query") (get params :query) (:query signals) "")
             :limit limit
             :offset offset}
      (or (get qp "types") (get params :types) (types-from-signals signals))
      (assoc :types (or (parse-types (or (get qp "types") (get params :types)))
                        (types-from-signals signals)))
      from (assoc :from from)
      to (assoc :to to)
      location (assoc :location location))))

(defn- hit-href
  [link-for-route hit]
  (when link-for-route
    (let [route (case (:type hit)
                  :member :members/by-id
                  :group :groups/by-id
                  :event :events/by-id
                  :post :posts/by-id
                  nil)]
      (when route
        (link-for-route route {:id (:id hit)})))))

(defn show-connect
  [{:keys [render layout link-for-route] :as deps} req]
  (let [command (command-from deps req)
        [_status {:keys [hits near-me-dropped?]}] (search/search deps command)
        hits (map #(assoc % :href (hit-href link-for-route %)) hits)
        patch-mode (or (get-in req [:query-params "patch-mode"])
                       (get-in req [:params :patch-mode])
                       "append")]
    (if (datastar-request? req)
      (with-sse req
        (fn [send!]
          (when (= "inner" patch-mode)
            (send! :html (presentation/location-bar (:location command))
                   {:selector "#connect-location"
                    :patch-mode "inner"}))
          (send! :html (or (presentation/near-me-notice near-me-dropped?) "")
                 {:selector "#connect-notice"
                  :patch-mode "inner"})
          (send! :html (map presentation/hit-card hits)
                 {:selector "#connect-hits"
                  :patch-mode patch-mode})
          (send! :signals {:offset (+ (:offset command) (:limit command))
                           :limit (:limit command)
                           :lat (get-in command [:location :lat])
                           :lng (get-in command [:location :lng])
                           :label (get-in command [:location :label] "")
                           :radius (get-in command [:location :radius-km] 100)
                           :geo (if (:location command) "" "off")})))
      {:status http-status/ok
       :body (-> {:hits hits
                  :near-me-dropped? near-me-dropped?
                  :query (:query command)
                  :types (:types command)
                  :period (or (get-in req [:query-params "period"])
                              (get-in req [:params :period])
                              "all")
                  :location (:location command)
                  :limit (:limit command)
                  :next-offset (+ (:offset command) (:limit command))}
                 presentation/present
                 layout
                 render)})))

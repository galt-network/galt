(println "CLJS: geocoding-map.cljs evaluated")

(require '["leaflet" :as L :refer [Map TileLayer Marker CircleMarker Circle]])

(def last-marker (atom nil))
(def last-map (atom nil))

(defn fly-to
  [map lat lng]
  (.flyTo map (clj->js [lat lng])))

(defn set-external-lat-long
  [lat lng]
  (let [lat-el (.getElementById js/document "latitude")
        lng-el (.getElementById js/document "longitude")]
    (set! (.-value lat-el) lat)
    (set! (.-value lng-el) lng)))

(defn new-custom-event [data]
  (new js/CustomEvent
       "update-coordinates"
       (clj->js {:detail data})))

(defn coordinates-from-marker
  [marker]
  [(.-lat (.getLatLng marker)) (.-lng (.getLatLng marker))])

(defn new-marker [lat lng & [options]]
  (let [marker (new Marker (clj->js [lat lng]) (clj->js (merge {:draggable true} options)))]
    (.on marker "moveend" (fn [event]
                            (.dispatchEvent
                              (.. marker -_map -_container)
                              (new-custom-event (coordinates-from-marker marker)))))))

(defn add-marker [map lat lng & [options]]
  (let [marker (new-marker lat lng options)]
    (reset! last-marker marker)
    (.addTo marker map)
    (set! (.-galtLastMarker js/window) marker)
    marker))

(defn lat-lng
  [lat lng]
  (clj->js [lat lng]))


(defn move-marker [map lat lng]
  (println ">>> move-marker " lat lng)
  (when (not @last-marker) (add-marker map lat lng))
  (.setLatLng @last-marker (lat-lng lat lng))
  (fly-to map lat lng))

(defn simple-centroid
  "Computes the arithmetic mean of lat/lng pairs"
  [coords]
  (let [lats (map first coords)
        lngs (map second coords)
        avg-lat (/ (reduce + lats) (count lats))
        avg-lng (/ (reduce + lngs) (count lngs))]
    [avg-lat avg-lng]))

(defn pan-to-contain [map coordinates]
  (let [coords (js->clj coordinates)
        latitudes (clojure.core/map first coords)
        longitudes (clojure.core/map second coords)
        min-lat (apply min latitudes)
        min-lng (apply min longitudes)
        max-lat (apply max latitudes)
        max-lng (apply max longitudes)
        bounds [[min-lat min-lng] [max-lat max-lng]]]
    ; (.panInsideBounds map (clj->js bounds))
    (.flyToBounds map (clj->js bounds))
    ))

(defn add-markers [map coordinates]
  (doall
    (clojure.core/map
      (fn [[lat lng popup-content]]
        (let [marker (add-marker map lat lng {:draggable false})]
          (.bindPopup marker popup-content)))
      (js->clj coordinates)))
  (pan-to-contain map coordinates))

(defn setup-map
  []
  (let [map (new Map "map")
        tile-params {:maxZoom 19 :attribution "http://www.openstreetmap.org/copyright"}
        tile-layer (new TileLayer "https://tile.openstreetmap.org/{z}/{x}/{y}.png" (clj->js tile-params))]
    (reset! last-map map)
    (.setPosition (.-zoomControl map) "bottomright")
    (.setView map (clj->js [51.505, -0.09]), 10)
    (.addTo tile-layer map)
    (set! (.-galtMap js/window) map)
    (set! (.-galtFly js/window) (partial fly-to map))
    (set! (.-galtMarker js/window) (partial add-marker map))
    (set! (.-galtMoveMarker js/window) (partial move-marker map))
    (set! (.-galtAddMarkers js/window) (partial add-markers map))
    ))

(setup-map)

; (add-marker @last-map 13.698993899755807, -89.19142489999999)
; (fly-to @last-map 13.698993899755807, -89.19142489999999)

(comment
  (println ">>> last-marker" last-marker)
  (add-marker @last-map 13.698993899755807, -89.19142489999999)
  (fly-to @last-map 13.698993899755807, -89.19142489999999)
  )

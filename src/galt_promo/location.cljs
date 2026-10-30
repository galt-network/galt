(ns galt-promo.location
  (:require [clojure.string :as str]
            ["geoip-lite" :as geoip]))

(defn parse-coordinates [lat lng]
  (when-not (or (str/blank? (str lat)) (str/blank? (str lng)))
    (let [lat (js/Number lat)
          lng (js/Number lng)]
      (when (and (js/Number.isFinite lat)
                 (js/Number.isFinite lng)
                 (<= -90 lat 90)
                 (<= -180 lng 180))
        {:lat lat :lng lng}))))

(defn ip-location [ip]
  (when-let [result (geoip/lookup ip)]
    (when-let [coordinates (.-ll result)]
      {:lat (aget coordinates 0)
       :lng (aget coordinates 1)
       :source :ip})))

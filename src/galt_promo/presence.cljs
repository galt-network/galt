(ns galt-promo.presence)

(def disconnect-grace-ms 10000)

(defn prune-disconnected [users now]
  (into {}
        (remove (fn [[_ {:keys [connected? disconnected-at]}]]
                  (and (not connected?)
                       (>= (- now disconnected-at) disconnect-grace-ms))))
        users))

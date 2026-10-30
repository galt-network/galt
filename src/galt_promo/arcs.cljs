(ns galt-promo.arcs)

(def online-color "#48c78e")
(def offline-color "#ff3860")

(defn- located? [{:keys [location]}]
  (and (number? (:lat location))
       (number? (:lng location))))

(defn- arcs-from [source users kind]
  (if (located? source)
    (for [target users
          :when (and (not= (:id source) (:id target))
                     (located? target))]
      {:source-id (:id source)
       :target-id (:id target)
       :startLat (get-in source [:location :lat])
       :startLng (get-in source [:location :lng])
       :endLat (get-in target [:location :lat])
       :endLng (get-in target [:location :lng])
       :color (if (= :online kind) online-color offline-color)
       :kind kind})
    []))

(defn transition-arcs [previous-users users]
  (if (nil? previous-users)
    []
    (let [previous-by-id (into {} (map (juxt :id identity)) previous-users)]
      (vec
       (mapcat
        (fn [user]
          (let [previous (get previous-by-id (:id user))]
            (cond
              (and (:connected? user) (not (:connected? previous)))
              (arcs-from user users :online)

              (and (not (:connected? user)) (:connected? previous))
              (arcs-from user users :offline)

              :else [])))
        users)))))

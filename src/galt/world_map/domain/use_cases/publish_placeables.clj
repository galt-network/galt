(ns galt.world-map.domain.use-cases.publish-placeables
  "Host-originated :placeable-map-objects push.
  With :user-id targets that user's own connections with their per-user
  list; without, broadcasts the base list to :everybody."
  (:require
   [is.galt.globo.protocols :as protocols]
   [is.galt.globo.server :as globo]))

(defn publish-placeables-use-case
  [{:keys [globo]} {:keys [user-id]}]
  (let [target (if user-id
                 (protocols/connection-ids-for-user (:storage globo) user-id)
                 :everybody)
        objects (protocols/placeable-objects (:placeables globo) user-id)]
    [:ok (globo/publish! globo target
                         {:type :placeable-map-objects
                          :content {:objects objects}})]))

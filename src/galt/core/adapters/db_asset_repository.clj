(ns galt.core.adapters.db-asset-repository
  (:require
    [galt.core.adapters.db-access :refer [query query-one]]
    [galt.core.adapters.db-result-transformations :refer [transform-row
                                                          defaults
                                                          default-datetime
                                                          map-without-nils]]
    [galt.core.domain.asset-repository :refer [AssetRepository]]
    [galt.core.domain.entities.asset :as asset]))

(def asset-spec
  {:assets/id defaults
   :assets/sha256 defaults
   :assets/content_type defaults
   :assets/size defaults
   :assets/original_filename defaults
   :assets/storage_key defaults
   :assets/visibility defaults
   :assets/owner_member_id defaults
   :assets/module defaults
   :assets/created_at default-datetime})

(defrecord DbAssetRepository [db-access]
  AssetRepository

  (add-asset [_ asset]
    (->> {:insert-into [:assets] :values [(map-without-nils asset)] :returning [:*]}
         (query-one db-access ,,,)
         (transform-row asset-spec ,,,)
         (asset/map->Asset ,,,)))

  (find-asset-by-id [_ id]
    (some->> {:select [:*] :from [:assets] :where [:= :id id]}
             (query-one db-access ,,,)
             (transform-row asset-spec ,,,)
             (asset/map->Asset ,,,)))

  (find-by-sha256-visibility [_ sha256 visibility]
    (some->> {:select [:*] :from [:assets]
              :where [:and [:= :sha256 sha256] [:= :visibility visibility]]}
             (query-one db-access ,,,)
             (transform-row asset-spec ,,,)
             (asset/map->Asset ,,,)))

  (find-by-storage-key [_ storage-key]
    (some->> {:select [:*] :from [:assets] :where [:= :storage_key storage-key]}
             (query-one db-access ,,,)
             (transform-row asset-spec ,,,)
             (asset/map->Asset ,,,)))

  (delete-asset [_ id]
    (->> {:delete-from [:assets] :where [:= :id id]}
         (query db-access ,,,)))

  (list-assets [_]
    (some->> {:select [:*] :from [:assets] :order-by [:created_at]}
             (query db-access ,,,)
             (map #(transform-row asset-spec %) ,,,)
             (map asset/map->Asset ,,,))))

(def last-repo (atom nil))
(defn new-db-asset-repository [db-access]
  (reset! last-repo (DbAssetRepository. db-access))
  (DbAssetRepository. db-access))

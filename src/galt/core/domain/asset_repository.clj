(ns galt.core.domain.asset-repository)

(defprotocol AssetRepository
  (add-asset [this asset])
  (find-asset-by-id [this id])
  (find-by-sha256-visibility [this sha256 visibility])
  (find-by-storage-key [this storage-key])
  (delete-asset [this id])
  (list-assets [this]))

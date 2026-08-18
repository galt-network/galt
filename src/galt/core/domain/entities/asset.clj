(ns galt.core.domain.entities.asset)

(defrecord Asset [id
                 sha256
                 content-type
                 size
                 original-filename
                 storage-key
                 visibility
                 owner-member-id
                 module
                 created-at])

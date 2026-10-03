(ns galt.connect.domain.search-repository)

(defprotocol SearchRepository
  (candidates [this command]))

(ns galt.locations.domain.location-repository)

(defprotocol LocationRepository
  (find-city-by-id [this id])
  (fuzzy-find-city [this s] [this s country-code])
  (all-countries [this])
  (fuzzy-find-country [this s])
  (find-country-by-code [this code])

  (add-location [this location])
  (find-location-by-id [this id])
  )

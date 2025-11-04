(ns galt.locations.domain.location-repository)

(defprotocol LocationRepository
  (find-city-by-id [this id])
  (fuzzy-find-city [this s] [this s country-code])
  (all-countries [this])
  (fuzzy-find-country [this s])
  (find-country-by-code [this code])

  (add-location [this location])
  (update-location [this location-id attributes])
  (find-location-by-id [this id])
  (locations-by-id [this id-or-many])
  (locations-within-bounds [this bounds] "Takes a vector opposite corner coordinate [[x1 y1] [x2 y2]]")
  )

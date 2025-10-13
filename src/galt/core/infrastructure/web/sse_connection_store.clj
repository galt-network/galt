(ns galt.core.infrastructure.web.sse-connection-store)

(defonce sse-connections (atom {}))

(defn add-connection
  ([id connection]
   (add-connection sse-connections id connection))
  ([store id connection]
   (swap! store assoc id connection)))

(defn get-connection
  ([id] (get-connection sse-connections id))
  ([store id] (get @store id)))

(defn remove-connection
  ([id] (remove-connection sse-connections id))
  ([store id] (swap! store dissoc id)))

(comment
  (require '[galt.core.adapters.sse-helpers :refer [send!]])
  sse-connections
  (send!
    (get-connection "membership-payment-019954c1-bc96-706d-85b2-4b9b01e197c7-2025092112440001")
    :signals
    {:payment-status "DONE!"}))

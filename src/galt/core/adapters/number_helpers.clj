(ns galt.core.adapters.number-helpers)

(defn ->int [s]
  (try
    (Integer/parseInt s)
    (catch NumberFormatException _e
      nil)))

(ns galt.core.adapters.number-helpers)

(defn ->int [s]
  (if (number? s)
    s
    (try
      (Integer/parseInt s)
      (catch NumberFormatException _e
        nil))))

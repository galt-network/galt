(ns galt.core.views.datastar-helpers
  (:require
    [clojure.string :as str]
    [camel-snake-kebab.core :as csk]))

(defn js-literal
  "Recursively converts clojure map into JavaScript object string with camelCase keys
  Has special handling for regex expressions within map values

  Example:
    (js-literal {:filter-signals {:excludeThings \"/files/\"}})
     ; => \"{filterSignals: {excludeThings: /files/}}\""
  [x]
  (cond
    (map? x)
    (str "{"
         (clojure.string/join ", "
           (for [[k v] x]
             (str (if (keyword? k) (csk/->camelCase (name k)) (str k)) ": " (js-literal v))))
         "}")

    (sequential? x)
    (str "["
         (clojure.string/join ", " (map js-literal x))
         "]")

    (nil? x) "null"
    (number? x) (str x)
    (boolean? x) (if x "true" "false")
    (string? x)
    (if (and (.startsWith x "/") (.endsWith x "/"))
      x  ; Output as regex literal without quotes
      (pr-str x))  ; Quote normal strings

    :else (pr-str x)))

(defn map->query-params
  "Converts a Clojure map into a URL query string."
  [m]
  (let [encoded-pairs (for [[k v] m]
                        (str (java.net.URLEncoder/encode (name k) "UTF-8")
                             "="
                             (java.net.URLEncoder/encode (str v) "UTF-8")))]
    (when (seq encoded-pairs)
      (str/join "&" encoded-pairs))))

(defn d*-backend-action
  "Returns a string with specified query method to given url with URL encoded query params

  Example:
    (d*-backend-action \"/my-endpoint\" :get {:hello 42 :bye 21})
      ; => @get('/my-endpoint?hello=42&bye=21')"
  ([url]
   (d*-backend-action url :get))
  ([url method]
   (d*-backend-action url method {}))
  ([url method params]
   (d*-backend-action url method params {}))
  ([url method params d*-params]
    (let [encoded-query-params (map->query-params params)
          d*-params-str (js-literal d*-params)]
      (str "@" (name method) "('" url "?" encoded-query-params "'," d*-params-str ");"))))


(comment
  (d*-backend-action "/end/of/the-world" :post {:windows 95 :name "unknown thing"})
  (require '[galt.core.infrastructure.web.helpers :refer [->json]])
  (js-literal {:filter-signals {:exclude-things "/files/"}})
  (d*-backend-action "/geocoding/search-cities" :get {:id "el.value"} {:filter-signals {:exclude "/files/"}}))

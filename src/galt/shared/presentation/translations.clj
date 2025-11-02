(ns galt.shared.presentation.translations
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]))

(defn read-edn-resource [resource-path]
  (let [url (io/resource resource-path)]
    (if url
      (edn/read-string (slurp url))
      (throw (ex-info "EDN resource not found" {:path resource-path})))))

;; Map of maps {:en {...english translations} :es {...spanish translations} ...}
(def translations (atom nil))
(def supported-languages [:en])

(defn- language->file-name
  [language]
  (str "translations/" (name language) ".edn"))

(defn load-translations
  [languages]
  (doall
    (for [language languages]
      (swap! translations
             (fn [t]
               (->> (language->file-name language)
                    (read-edn-resource ,,,)
                    (assoc t language ,,,)))))))

(defn i18n
  ([key] (i18n :en key))
  ([language key]
   (when (or
           (nil? @translations)
           (= (System/getenv "GALT_ENV") "dev")) (load-translations supported-languages))
   (get-in @translations [language key])))

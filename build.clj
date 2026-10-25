(ns build
  (:require [clojure.tools.build.api :as b]))

(def lib 'galt)
;; .dockerignore drops .git, so image builds fall back to 0.
(def version (format "0.1.%s" (try (b/git-count-revs nil) (catch Exception _ 0))))
(def class-dir "target/classes")
(def uber-file (format "target/%s-%s-standalone.jar" (name lib) version))

;; delay to defer side effects (artifact downloads)
;; Image builds set GLOBO_LOCAL_ROOT to a checkout that contains compiled JS.
(def basis
  (delay (b/create-basis
           (cond-> {:project "deps.edn"}
             (System/getenv "GLOBO_LOCAL_ROOT")
             (assoc :extra {:deps {'is.mad/globo {:local/root (System/getenv "GLOBO_LOCAL_ROOT")}}})))))

(defn clean [_]
  (b/delete {:path "target"}))

(defn get-commit-hash
  []
  (try (b/git-process {:git-args "rev-parse --short HEAD"})
       (catch Exception _ "unknown")))

(defn collect-build-info
  []
  {:git-commit-hash (get-commit-hash)})

(defn uber [_]
  (clean nil)
  (b/copy-dir {:src-dirs ["src" "resources"]
               :target-dir class-dir})
  (spit (str class-dir "/build-info.edn") (pr-str (collect-build-info)))
  (b/compile-clj {:basis @basis
                  :ns-compile '[galt.main]
                  :class-dir class-dir})
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis @basis
           :main 'galt.main}))

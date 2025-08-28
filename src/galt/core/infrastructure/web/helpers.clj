(ns galt.core.infrastructure.web.helpers
  (:require
    [galt.core.adapters.view-models :as core-view-models]
    [galt.core.views.layout :as layout]
    [hiccup2.core :as h]
    [starfederation.datastar.clojure.api :as d*]
    [charred.api :as charred]
    [clojure.walk]
    [hiccup.page]))

(defn no-layout [req content] (content req))

(defn render-html-page
  [hiccup]
  (-> (h/html (:html5 hiccup.page/doctype) hiccup) str))

(defn render-html
  [hiccup]
  (-> hiccup (h/html ,,,) (str ,,,)))

(defn layout-for-content
  [layout-view base-model additional-props-or-content]
  (layout-view (merge
                 base-model
                 (if (map? additional-props-or-content)
                   additional-props-or-content
                   {:content additional-props-or-content}))))

(defn with-layout
  [deps handler]
  (fn [req]
    (let [layout-model (core-view-models/layout-model deps req)]
      (handler (merge deps
                      {:layout (partial layout-for-content layout/main-layout layout-model)
                       :navbar layout/navbar
                       :content layout/content
                       :update-layout-model (fn [req] (core-view-models/layout-model deps req))
                       :layout-model layout-model}) req))))

(def ^:private bufSize 1024)
(def read-json (charred/parse-json-fn {:async? false :bufsize bufSize}))
(def ->json charred/write-json-str)

(defn get-signals [req]
  (some-> req d*/get-signals read-json clojure.walk/keywordize-keys))

(comment
  (charred/write-json-str {:hello "World"}))

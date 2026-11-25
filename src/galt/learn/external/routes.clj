(ns galt.learn.external.routes
  (:require [galt.learn.adapters.handlers :as learn]
            [reitit.ring :as rr]))

(defn router
  [deps]
  (let [with-layout (:with-new-layout deps)
        with-deps-layout (partial with-layout deps)]
    (rr/router
      [["/learn" {:id :learn :name :learn :get (with-deps-layout learn/show-learn)}]
       ["/learn/:module-slug/:lesson-slug"
        {:id :learn :name :learn/lesson :get (with-deps-layout learn/show-lesson)}]])))

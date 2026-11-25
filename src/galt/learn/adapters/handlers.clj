(ns galt.learn.adapters.handlers
  (:require [galt.learn.adapters.curriculum :as data]
            [galt.learn.adapters.presentation.learn :as learn]
            [galt.learn.adapters.presentation.lesson :as lesson]
            [ring.util.http-status :as http-status]))

(defn show-learn
  [{:keys [render layout curriculum]} _req]
  {:status http-status/ok
   :body (-> (curriculum) learn/present layout render)})

(defn show-lesson
  [{:keys [render layout curriculum]} req]
  (let [{:keys [module-slug lesson-slug]} (:path-params req)]
    (if-let [found (data/find-lesson (curriculum) module-slug lesson-slug)]
      {:status http-status/ok
       :body (-> found lesson/present layout render)}
      {:status http-status/not-found
       :body (-> (lesson/not-found) layout render)})))

(ns galt.learn.adapters.handlers
  (:require [galt.learn.adapters.presentation.learn :as learn]
            [ring.util.http-status :as http-status]))

(defn show-learn
  [{:keys [render layout curriculum]} _req]
  {:status http-status/ok
   :body (-> (curriculum) learn/present layout render)})
